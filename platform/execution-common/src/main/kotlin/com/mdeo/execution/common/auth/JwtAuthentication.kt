package com.mdeo.execution.common.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Default authentication name for JWT-based authentication.
 */
const val AUTH_JWT = "auth-jwt"

private val logger = LoggerFactory.getLogger("JwtAuth")

/**
 * Configures JWT authentication for an execution service using JwkProvider.
 *
 * This implementation follows the Ktor JWT guide recommendations for RS256:
 * - Uses JwkProviderBuilder to automatically fetch and cache JWKS from the backend
 * - The provider handles key rotation automatically by fetching from /.well-known/jwks.json
 * - Includes caching (10 keys for 24 hours) and rate limiting (10 requests per minute)
 *
 * @param backendUrl The base URL of the backend service (e.g., "http://localhost:8080")
 * @param issuer The expected JWT issuer
 *
 * @see <a href="https://ktor.io/docs/server-jwt.html">Ktor JWT Authentication Guide</a>
 */
fun Application.configureJwtAuth(
    backendUrl: String,
    issuer: String
) {
    logger.info("Configuring JWT authentication with backend URL: $backendUrl")
    logger.info("JWKS endpoint: $backendUrl/.well-known/jwks.json")

    val jwkProvider = JwkProviderBuilder(backendUrl)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt(AUTH_JWT) {
            verifier(jwkProvider, issuer) {
                acceptLeeway(3)
            }

            validate { credential ->
                validateJwtCredential(credential)
            }
        }
    }

    logger.info("JWT authentication configured successfully")
}

/**
 * Validates JWT credentials and creates a principal if valid.
 *
 * @param credential The JWT credential to validate
 * @return JWTPrincipal if valid, null otherwise
 */
private fun validateJwtCredential(credential: JWTCredential): JWTPrincipal? {
    return try {
        val projectId = credential.payload.getClaim("projectId")?.asString()
        val executionId = credential.payload.getClaim("executionId")?.asString()
        
        logger.info("JWT validated for projectId: $projectId, executionId: $executionId")
        JWTPrincipal(credential.payload)
    } catch (e: Exception) {
        logger.error("Token validation failed", e)
        null
    }
}
