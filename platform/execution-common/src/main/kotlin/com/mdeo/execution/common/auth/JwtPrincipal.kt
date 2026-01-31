package com.mdeo.execution.common.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

/**
 * Represents the extracted claims from a JWT token.
 * Contains common claims used by execution services.
 *
 * @property projectId The project ID from the token claims
 * @property executionId The execution ID from the token claims
 * @property scopes The list of scopes/permissions granted by the token
 */
data class JwtPrincipalData(
    val projectId: String?,
    val executionId: String?,
    val scopes: List<String>
)

/**
 * Extension function to extract JWT principal data from the call.
 * Extracts common claims (projectId, executionId, scopes) from the JWT.
 *
 * @return JwtPrincipalData if a valid JWT principal exists, null otherwise
 */
fun ApplicationCall.getJwtPrincipalData(): JwtPrincipalData? {
    val jwtPrincipal = principal<JWTPrincipal>() ?: return null

    val projectId = jwtPrincipal.payload.getClaim("projectId")?.asString()
    val executionId = jwtPrincipal.payload.getClaim("executionId")?.asString()
    val scopes = jwtPrincipal.payload.getClaim("scope")?.asList(String::class.java) ?: emptyList()

    return JwtPrincipalData(projectId, executionId, scopes)
}

/**
 * Extension function to get the raw JWT token from the Authorization header.
 *
 * @return The JWT token string if present, null otherwise
 */
fun ApplicationCall.getJwtToken(): String? {
    val authHeader = request.headers[HttpHeaders.Authorization] ?: return null
    if (!authHeader.startsWith("Bearer ")) return null
    return authHeader.substring(7)
}

/**
 * Checks if the principal has the specified scope.
 *
 * @param scope The scope to check for
 * @return true if the principal has the scope, false otherwise
 */
fun JwtPrincipalData.hasScope(scope: String): Boolean {
    return scopes.contains(scope)
}

/**
 * Checks if the principal has any of the specified scopes.
 *
 * @param requiredScopes The scopes to check for
 * @return true if the principal has at least one of the scopes
 */
fun JwtPrincipalData.hasAnyScope(vararg requiredScopes: String): Boolean {
    return requiredScopes.any { scopes.contains(it) }
}

/**
 * Checks if the principal has all of the specified scopes.
 *
 * @param requiredScopes The scopes to check for
 * @return true if the principal has all of the scopes
 */
fun JwtPrincipalData.hasAllScopes(vararg requiredScopes: String): Boolean {
    return requiredScopes.all { scopes.contains(it) }
}
