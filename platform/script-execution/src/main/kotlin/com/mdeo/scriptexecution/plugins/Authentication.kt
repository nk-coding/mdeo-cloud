package com.mdeo.scriptexecution.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

const val AUTH_JWT = "auth-jwt"

private val logger = LoggerFactory.getLogger("JwtAuth")

/**
 * Configures JWT authentication for the application using JwkProvider.
 * 
 * This implementation follows the Ktor JWT guide recommendations for RS256:
 * - Uses JwkProviderBuilder to automatically fetch and cache JWKS from the backend
 * - The provider handles key rotation automatically by fetching from /.well-known/jwks.json
 * - Includes caching (10 keys for 24 hours) and rate limiting (10 requests per minute)
 * - Much simpler than manual JWKS fetching and RSA key construction
 * 
 * @param backendUrl The base URL of the backend service (e.g., "http://localhost:8080")
 * 
 * @see <a href="https://ktor.io/docs/server-jwt.html">Ktor JWT Authentication Guide</a>
 */
fun Application.configureJwtAuth(
    backendUrl: String
) {
    logger.info("Configuring JWT authentication with backend URL: $backendUrl")
    logger.info("JWKS endpoint: $backendUrl/.well-known/jwks.json")

    val jwkProvider = JwkProviderBuilder(backendUrl)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt(AUTH_JWT) {
            verifier(jwkProvider, backendUrl) {
                acceptLeeway(3)
            }

            validate { credential ->
                try {
                    val projectId = credential.payload.getClaim("projectId")?.asString()
                    val executionId = credential.payload.getClaim("executionId")?.asString()

                    logger.debug("JWT validated for projectId: $projectId, executionId: $executionId")
                    JWTPrincipal(credential.payload)
                } catch (e: Exception) {
                    logger.error("Token validation failed", e)
                    null
                }
            }
        }
    }

    logger.info("JWT authentication configured successfully")
}

/**
 * Extension function to get JWT principal from the call.
 */
fun ApplicationCall.getJwtPrincipal(): JwtPrincipal? {
    val jwtPrincipal = principal<JWTPrincipal>() ?: return null

    val projectId = jwtPrincipal.payload.getClaim("projectId")?.asString()
    val executionId = jwtPrincipal.payload.getClaim("executionId")?.asString()
    val scopes = jwtPrincipal.payload.getClaim("scope")?.asList(String::class.java) ?: emptyList()

    return JwtPrincipal(projectId, executionId, scopes)
}

/**
 * Extension function to get the JWT token from the Authorization header.
 */
fun ApplicationCall.getJwtToken(): String? {
    val authHeader = request.headers[io.ktor.http.HttpHeaders.Authorization] ?: return null
    if (!authHeader.startsWith("Bearer ")) return null
    return authHeader.substring(7)
}

/**
 * JWT Principal containing claims from the token.
 */
data class JwtPrincipal(
    val projectId: String?,
    val executionId: String?,
    val scopes: List<String>
)
