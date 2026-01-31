package com.mdeo.execution.common.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures standardized health check route for execution services.
 * Provides a simple endpoint for Docker health checks and load balancer probes.
 */
fun Route.healthRoutes() {
    get("/health") {
        call.respond(
            HttpStatusCode.OK,
            mapOf("status" to "running")
        )
    }
}

/**
 * Configures health check route with database connectivity check.
 *
 * @param checkDatabase Function that returns true if database is healthy
 */
fun Route.healthRoutes(checkDatabase: () -> Boolean) {
    get("/health") {
        val dbHealthy = checkDatabase()
        val status = if (dbHealthy) "running" else "degraded"
        val httpStatus = if (dbHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

        call.respond(
            httpStatus,
            mapOf(
                "status" to status,
                "database" to if (dbHealthy) "connected" else "disconnected"
            )
        )
    }
}
