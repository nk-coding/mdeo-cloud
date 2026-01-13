package com.mdeo.backend.plugins

import com.auth0.jwt.interfaces.Payload
import com.mdeo.backend.service.JwtService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.util.*

const val AUTH_SESSION = "auth-session"
const val AUTH_JWT = "auth-jwt"

/**
 * Principal for JWT authentication containing project access information.
 */
data class JwtPrincipal(
    val projectId: String,
    val scopes: List<String>,
    val payload: Payload
)

/**
 * Configure session-based authentication using Ktor's built-in session authentication.
 *
 * Validates that a UserSession exists and returns an unauthorized response if validation fails.
 */
fun Application.configureAuthentication(jwtService: JwtService) {
    install(Authentication) {
        session<UserSession>(AUTH_SESSION) {
            validate { session ->
                session
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
            }
        }
        
        jwt(AUTH_JWT) {
            verifier(jwtService.getVerifier())
            validate { credential ->
                val projectId = credential.payload.getClaim(JwtService.CLAIM_PROJECT_ID)?.asString()
                
                if (projectId != null) {
                    val scopes = credential.payload.getClaim(JwtService.CLAIM_SCOPE)
                        ?.asList(String::class.java) ?: emptyList()
                    JwtPrincipal(projectId, scopes, credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired token"))
            }
        }
    }
}

/**
 * Extension function to retrieve the current user session from the application call.
 *
 * @return The current UserSession if authenticated, null otherwise
 */
fun ApplicationCall.getUserSession(): UserSession? {
    return principal<UserSession>()
}

/**
 * Extension function to retrieve the JWT principal from the application call.
 *
 * @return The JwtPrincipal if authenticated via JWT, null otherwise
 */
fun ApplicationCall.getJwtPrincipal(): JwtPrincipal? {
    return principal<JwtPrincipal>()
}

/**
 * Extension function to check if the current user has administrator privileges.
 *
 * @return true if the user is authenticated and has admin role, false otherwise
 */
fun ApplicationCall.isAdmin(): Boolean {
    return getUserSession()?.isAdmin == true
}
