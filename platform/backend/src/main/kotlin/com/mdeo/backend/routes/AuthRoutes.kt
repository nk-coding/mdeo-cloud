package com.mdeo.backend.routes

import com.mdeo.backend.plugins.*
import com.mdeo.backend.service.UserService
import com.mdeo.common.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

/**
 * Configures authentication routes.
 *
 * @param userService Service for user authentication and management
 */
fun Route.authRoutes(userService: UserService) {
    route("/api/auth") {
        /**
         * Authenticates a user and creates a session.
         *
         * @param body LoginRequest containing username and password
         * @return LoginResponse with user information on success, 401 Unauthorized on failure
         */
        post("/login") {
            val request = call.receive<LoginRequest>()
            
            val user = userService.verifyPassword(request.username, request.password)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }
            
            call.sessions.set(UserSession(
                userId = user.id,
                username = user.username,
                isAdmin = user.roles.contains("admin")
            ))
            
            call.respond(LoginResponse(UserInfo(
                id = user.id,
                username = user.username,
                isAdmin = user.roles.contains("admin")
            )))
        }

        /**
         * Registers a new user account and creates a session.
         */
        post("/register") {
            val request = call.receive<RegisterRequest>()

            if (request.username.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Username and password are required"))
                return@post
            }

            val user = userService.createUser(request.username, request.password)
            if (user == null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Username already exists"))
                return@post
            }

            call.sessions.set(UserSession(
                userId = user.id,
                username = user.username,
                isAdmin = user.roles.contains("admin")
            ))

            call.respond(LoginResponse(UserInfo(
                id = user.id,
                username = user.username,
                isAdmin = user.roles.contains("admin")
            )))
        }
        
        /**
         * Logs out the current user by clearing their session.
         *
         * @return 200 OK with success message
         */
        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out"))
        }
        
        /**
         * Gets the current authenticated user's information.
         *
         * @return ApiResult.Success with UserInfo if authenticated, 401 Unauthorized otherwise
         */
        get("/me") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResult.Failure(ApiError("NOT_AUTHENTICATED", "Not authenticated")))
            } else {
                call.respond(ApiResult.Success(UserInfo(
                    id = session.userId,
                    username = session.username,
                    isAdmin = session.isAdmin
                )))
            }
        }
    }
}
