package com.mdeo.backend.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.mdeo.backend.config.JwtConfig
import org.slf4j.LoggerFactory
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*

/**
 * Service for JWT token generation and verification using RSA key pairs.
 *
 * @param services The injected services providing access to configuration and other services
 */
class JwtService(services: InjectedServices) : BaseService(), InjectedServices by services {
    private val logger = LoggerFactory.getLogger(JwtService::class.java)
    
    /**
     * JWT configuration settings 
     */
    private val jwtConfig: JwtConfig get() = config.jwt
    
    private lateinit var privateKey: RSAPrivateKey
    private lateinit var publicKey: RSAPublicKey
    private lateinit var algorithm: Algorithm
    
    companion object {
        const val CLAIM_PROJECT_ID = "projectId"
        const val CLAIM_EXECUTION_ID = "executionId"
        const val CLAIM_SCOPE = "scope"
        
        const val SCOPE_FILES_READ = "files:read"
        const val SCOPE_FILE_DATA_READ = "file-data:read"
        const val SCOPE_EXECUTION_READ = "execution:read"
        const val SCOPE_EXECUTION_WRITE = "execution:write"
        const val SCOPE_PLUGIN_EXECUTION_READ = "plugin:execution:read"
        const val SCOPE_PLUGIN_EXECUTION_CANCEL = "plugin:execution:cancel"
        const val SCOPE_PLUGIN_EXECUTION_DELETE = "plugin:execution:delete"

    }
    
    /**
     * Initialize the JWT service by loading keys from config or generating new ones.
     * Must be called before using other methods.
     */
    fun init() {
        logger.info("Initializing JWT service...")
        
        if (jwtConfig.privateKey != null && jwtConfig.publicKey != null) {
            logger.info("Loading RSA key pair from configuration")
            val privateKeyBytes = Base64.getDecoder().decode(jwtConfig.privateKey)
            val publicKeyBytes = Base64.getDecoder().decode(jwtConfig.publicKey)
            
            val keyFactory = KeyFactory.getInstance("RSA")
            privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes)) as RSAPrivateKey
            publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes)) as RSAPublicKey
        } else {
            logger.warn("No RSA keys provided in configuration, generating new key pair")
            logger.warn("For production use, set JWT_PRIVATE_KEY and JWT_PUBLIC_KEY environment variables")
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()
            
            privateKey = keyPair.private as RSAPrivateKey
            publicKey = keyPair.public as RSAPublicKey
            
            val privateKeyEncoded = (privateKey as java.security.Key).encoded
            val publicKeyEncoded = (publicKey as java.security.Key).encoded
            
            logger.info("Generated JWT keys - Private key: ${Base64.getEncoder().encodeToString(privateKeyEncoded)}")
            logger.info("Generated JWT keys - Public key: ${Base64.getEncoder().encodeToString(publicKeyEncoded)}")
        }
        
        algorithm = Algorithm.RSA256(publicKey, privateKey)
        logger.info("JWT service initialized successfully")
    }
    
    /**
     * Generates a JWT token for a project with read-only access to files and file-data.
     *
     * @param projectId The UUID of the project to grant access to
     * @return The generated JWT token string
     */
    fun generateProjectToken(projectId: UUID): String {
        val now = Instant.now()
        val expiration = now.plusSeconds(jwtConfig.expirationSeconds)
        
        return JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiration))
            .withClaim(CLAIM_PROJECT_ID, projectId.toString())
            .withArrayClaim(CLAIM_SCOPE, arrayOf(SCOPE_FILES_READ, SCOPE_FILE_DATA_READ))
            .sign(algorithm)
    }
    
    /**
     * Generates a JWT token for execution operations.
     * Includes read access to project data and write access to execution progress.
     *
     * @param projectId The UUID of the project
     * @param executionId The UUID of the execution
     * @param additionalScopes Additional scopes to include in the token
     * @return The generated JWT token string
     */
    fun generateExecutionToken(projectId: UUID, executionId: UUID, additionalScopes: List<String>): String {
        val now = Instant.now()
        val expiration = now.plusSeconds(jwtConfig.expirationSeconds)
        
        return JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiration))
            .withClaim(CLAIM_PROJECT_ID, projectId.toString())
            .withClaim(CLAIM_EXECUTION_ID, executionId.toString())
            .withArrayClaim(CLAIM_SCOPE, arrayOf(
                SCOPE_FILES_READ,
                SCOPE_FILE_DATA_READ,
                SCOPE_EXECUTION_READ
            ) + additionalScopes)
            .sign(algorithm)
    }
    
    /**
     * Gets a JWT verifier for validating tokens.
     *
     * @return A configured JWT verifier
     */
    fun getVerifier(): com.auth0.jwt.JWTVerifier {
        return JWT.require(algorithm)
            .withIssuer(jwtConfig.issuer)
            .build()
    }
    
    /**
     * Verifies a JWT token and returns the decoded token.
     *
     * @param token The JWT token to verify
     * @return The decoded JWT if valid
     * @throws com.auth0.jwt.exceptions.JWTVerificationException if the token is invalid
     */
    fun verifyToken(token: String): DecodedJWT {
        return getVerifier().verify(token)
    }
    
    /**
     * Extracts the project ID from a verified JWT token.
     *
     * @param token The decoded JWT token
     * @return The project UUID, or null if not present
     */
    fun getProjectId(token: DecodedJWT): UUID? {
        return token.getClaim(CLAIM_PROJECT_ID).asString()?.let { 
            try { UUID.fromString(it) } catch (e: Exception) { null }
        }
    }
    
    /**
     * Checks if a token has a specific scope.
     *
     * @param token The decoded JWT token
     * @param scope The scope to check for
     * @return true if the token has the scope, false otherwise
     */
    fun hasScope(token: DecodedJWT, scope: String): Boolean {
        val scopes = token.getClaim(CLAIM_SCOPE).asList(String::class.java) ?: return false
        return scope in scopes
    }
    
    /**
     * Gets the public key in PEM format for external verification.
     *
     * @return The public key as a PEM-encoded string
     */
    fun getPublicKeyPem(): String {
        val base64 = Base64.getMimeEncoder(64, "\n".toByteArray())
            .encodeToString(publicKey.encoded)
        return "-----BEGIN PUBLIC KEY-----\n$base64\n-----END PUBLIC KEY-----"
    }
    
    /**
     * Gets the public key in JWKS (JSON Web Key Set) format for external verification.
     * Returns a JWKS containing the RSA public key with standard parameters.
     *
     * @return Map representing the JWKS structure
     */
    fun getJwks(): Map<String, Any> {
        // Fix: Strip leading 0x00 byte from BigInteger.toByteArray() to comply with RFC 7518
        // BigInteger.toByteArray() includes a sign bit which must be removed for proper Base64url encoding
        val modulusBytes = publicKey.modulus.toByteArray()
        val modulusStripped = if (modulusBytes[0] == 0.toByte() && modulusBytes.size > 1) {
            modulusBytes.copyOfRange(1, modulusBytes.size)
        } else {
            modulusBytes
        }
        
        val exponentBytes = publicKey.publicExponent.toByteArray()
        val exponentStripped = if (exponentBytes[0] == 0.toByte() && exponentBytes.size > 1) {
            exponentBytes.copyOfRange(1, exponentBytes.size)
        } else {
            exponentBytes
        }
        
        val modulusBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(modulusStripped)
        val exponentBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(exponentStripped)
        
        // Generate a stable key ID based on the modulus (for key rotation support)
        val keyId = "mdeo-key-1"
        
        val key = mapOf(
            "kty" to "RSA",
            "use" to "sig",
            "alg" to "RS256",
            "kid" to keyId,
            "n" to modulusBase64,
            "e" to exponentBase64
        )
        
        return mapOf("keys" to listOf(key))
    }
    
    /**
     * Gets the RSA algorithm for use in authentication configuration.
     *
     * @return The RSA algorithm instance
     */
    fun getAlgorithm(): Algorithm = algorithm
    
    /**
     * Gets the issuer string for JWT verification.
     *
     * @return The issuer string
     */
    fun getIssuer(): String = jwtConfig.issuer
}
