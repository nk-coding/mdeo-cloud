package com.mdeo.scriptexecution.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures health check route.
 * Simple endpoint for Docker health checks.
 */
fun Route.healthRoutes() {
    get("/health") {
        call.respond(
            HttpStatusCode.OK,
            mapOf("status" to "running")
        )
    }
}
