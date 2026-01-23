package com.mdeo.scriptexecution.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

const val AUTH_JWT = "auth-jwt"

private val logger = LoggerFactory.getLogger("JwtAuth")

/**
 * JWKS response from the backend.
 */
@Serializable
data class JwksResponse(
    val keys: List<JwkKey>
)

/**
 * Individual JWK key.
 */
@Serializable
data class JwkKey(
    val kty: String,
    val use: String,
    val kid: String,
    val n: String,
    val e: String
)

/**
 * JWT Principal containing claims from the token.
 */
data class JwtPrincipal(
    val subject: String,
    val projectId: String?,
    val executionId: String?,
    val scopes: List<String>
)

/**
 * Fetches the JWKS from the backend and returns a map of kid -> RSAPublicKey.
 */
suspend fun fetchJwks(client: HttpClient, backendUrl: String): Map<String, RSAPublicKey> {
    return try {
        val response = client.get("$backendUrl/.well-known/jwks.json")
        if (response.status != HttpStatusCode.OK) {
            logger.error("Failed to fetch JWKS: ${response.status}")
            return emptyMap()
        }
        
        val jwks = response.body<JwksResponse>()
        jwks.keys.associate { key ->
            key.kid to rsaPublicKeyFromJwk(key.n, key.e)
        }
    } catch (e: Exception) {
        logger.error("Error fetching JWKS", e)
        emptyMap()
    }
}

/**
 * Converts JWK parameters (n, e) to an RSAPublicKey.
 */
private fun rsaPublicKeyFromJwk(n: String, e: String): RSAPublicKey {
    val modulus = Base64.getUrlDecoder().decode(n)
    val exponent = Base64.getUrlDecoder().decode(e)
    
    val keySpec = X509EncodedKeySpec(
        createX509EncodedKey(modulus, exponent)
    )
    
    val keyFactory = KeyFactory.getInstance("RSA")
    return keyFactory.generatePublic(keySpec) as RSAPublicKey
}

/**
 * Creates an X.509 encoded key from RSA modulus and exponent.
 */
private fun createX509EncodedKey(modulus: ByteArray, exponent: ByteArray): ByteArray {
    // DER encoding of RSA public key
    val modulusLength = modulus.size
    val exponentLength = exponent.size
    
    // Calculate sizes
    val modulusTlv = byteArrayOf(0x02) + encodeLength(modulusLength) + modulus
    val exponentTlv = byteArrayOf(0x02) + encodeLength(exponentLength) + exponent
    val sequenceContent = modulusTlv + exponentTlv
    val sequenceTlv = byteArrayOf(0x30) + encodeLength(sequenceContent.size) + sequenceContent
    
    // Wrap in BIT STRING
    val bitStringContent = byteArrayOf(0x00) + sequenceTlv
    val bitStringTlv = byteArrayOf(0x03) + encodeLength(bitStringContent.size) + bitStringContent
    
    // Algorithm identifier for RSA
    val algorithmOid = byteArrayOf(0x06, 0x09, 0x2a.toByte(), 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01)
    val algorithmNull = byteArrayOf(0x05, 0x00)
    val algorithmSequence = byteArrayOf(0x30) + encodeLength(algorithmOid.size + algorithmNull.size) + algorithmOid + algorithmNull
    
    // Final sequence
    val finalContent = algorithmSequence + bitStringTlv
    return byteArrayOf(0x30) + encodeLength(finalContent.size) + finalContent
}

/**
 * Encodes length in DER format.
 */
private fun encodeLength(length: Int): ByteArray {
    return if (length < 128) {
        byteArrayOf(length.toByte())
    } else {
        val lengthBytes = mutableListOf<Byte>()
        var temp = length
        while (temp > 0) {
            lengthBytes.add(0, (temp and 0xFF).toByte())
            temp = temp shr 8
        }
        byteArrayOf((0x80 or lengthBytes.size).toByte()) + lengthBytes.toByteArray()
    }
}

/**
 * Configures JWT authentication for the application.
 */
fun Application.configureJwtAuth(client: HttpClient, backendUrl: String, jwksKeys: Map<String, RSAPublicKey>) {
    install(Authentication) {
        jwt(AUTH_JWT) {
            // Create a verifier that can handle multiple keys
            // We'll use the first key as a fallback, but ideally we'd match by kid
            val firstKey = jwksKeys.values.firstOrNull()
            if (firstKey != null) {
                verifier(
                    JWT.require(Algorithm.RSA256(firstKey, null))
                        .build()
                )
            }
            
            validate { credential ->
                try {
                    // Try to verify with the correct key based on kid
                    val kid = credential.payload.getClaim("kid")?.asString()
                    val publicKey = if (kid != null) jwksKeys[kid] else jwksKeys.values.firstOrNull()
                    
                    if (publicKey == null) {
                        logger.warn("No public key found for kid: $kid")
                        return@validate null
                    }
                    
                    val subject = credential.payload.subject
                    val projectId = credential.payload.getClaim("projectId").asString()
                    val executionId = credential.payload.getClaim("executionId").asString()
                    val scopesClaim = credential.payload.getClaim("scope").asString()
                    val scopes = scopesClaim?.split(" ") ?: emptyList()
                    
                    if (subject != null) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    logger.error("Token validation failed", e)
                    null
                }
            }
        }
    }
}

/**
 * Extension function to get JWT principal from the call.
 */
fun ApplicationCall.getJwtPrincipal(): JwtPrincipal? {
    val jwtPrincipal = principal<JWTPrincipal>() ?: return null
    
    val subject = jwtPrincipal.payload.subject ?: return null
    val projectId = jwtPrincipal.payload.getClaim("projectId")?.asString()
    val executionId = jwtPrincipal.payload.getClaim("executionId")?.asString()
    val scopesClaim = jwtPrincipal.payload.getClaim("scope")?.asString()
    val scopes = scopesClaim?.split(" ") ?: emptyList()
    
    return JwtPrincipal(subject, projectId, executionId, scopes)
}

/**
 * Extension function to get the JWT token from the Authorization header.
 */
fun ApplicationCall.getJwtToken(): String? {
    val authHeader = request.headers[io.ktor.http.HttpHeaders.Authorization] ?: return null
    if (!authHeader.startsWith("Bearer ")) return null
    return authHeader.substring(7)
}
