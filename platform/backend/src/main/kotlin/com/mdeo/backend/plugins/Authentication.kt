package com.mdeo.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

const val AUTH_SESSION = "auth-session"

/**
 * Configure session-based authentication using Ktor's built-in session authentication.
 *
 * Validates that a UserSession exists and returns an unauthorized response if validation fails.
 */
fun Application.configureAuthentication() {
    install(Authentication) {
        session<UserSession>(AUTH_SESSION) {
            validate { session ->
                session
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
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
 * Extension function to check if the current user has administrator privileges.
 *
 * @return true if the user is authenticated and has admin role, false otherwise
 */
fun ApplicationCall.isAdmin(): Boolean {
    return getUserSession()?.isAdmin == true
}
