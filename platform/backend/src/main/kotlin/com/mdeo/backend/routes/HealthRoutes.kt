package com.mdeo.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures health check routes.
 * Simple endpoint for Docker health checks and monitoring.
 */
fun Route.healthRoutes() {
    /**
     * GET /health
     * Returns simple health status for Docker health checks.
     */
    get("/health") {
        call.respond(
            HttpStatusCode.OK,
            mapOf("status" to "running")
        )
    }
}
