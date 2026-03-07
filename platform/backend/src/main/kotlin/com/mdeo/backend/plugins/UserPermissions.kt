package com.mdeo.backend.plugins

import com.mdeo.backend.service.UserService
import com.mdeo.common.model.UserRoles
import io.ktor.server.application.*
import io.ktor.util.*
import java.util.UUID

/**
 * Holds the resolved global permissions for the currently authenticated user.
 *
 * Instances are cached per-request in [ApplicationCall.attributes] to avoid
 * redundant database lookups within the same request lifecycle.
 *
 * @property isAdmin Whether the user has global administrator privileges.
 * @property canCreateProject Whether the user is allowed to create new projects globally.
 */
data class UserPermissions(
    val isAdmin: Boolean,
    val canCreateProject: Boolean
) {
    companion object {
        /**
         * Sentinel instance representing an unauthenticated or unknown user.
         */
        val NONE = UserPermissions(isAdmin = false, canCreateProject = false)
    }
}

/**
 * Per-request cache key for [UserPermissions].
 */
private val USER_PERMISSIONS_KEY = AttributeKey<UserPermissions>("UserPermissions")

/**
 * Application-level key used to make [UserService] accessible to per-request resolution.
 */
internal val USER_SERVICE_KEY = AttributeKey<UserService>("UserService")

/**
 * Registers [userService] in application-level attributes so that
 * [ApplicationCall.getUserPermissions] can resolve user permissions without
 * explicit parameter threading through every route handler.
 *
 * Must be called once during application setup, before any requests are handled.
 */
fun Application.registerUserService(userService: UserService) {
    attributes.put(USER_SERVICE_KEY, userService)
}

/**
 * Resolves and caches the global permissions for the currently authenticated user.
 *
 * The result is computed once per request and stored in [ApplicationCall.attributes],
 * so repeated calls within the same request return the cached value without additional
 * database queries.
 *
 * Permission resolution strategy:
 * - **Session-authenticated requests** (`AUTH_SESSION`): the user's roles are fetched fresh
 *   from the database, ensuring that permission changes take effect on the very next request
 *   without requiring the session cookie to be renewed.
 * - **JWT-authenticated requests** (`AUTH_JWT`): project-scoped tokens carry no user identity,
 *   so [UserPermissions.NONE] is returned.
 * - **Unauthenticated / unresolvable user**: [UserPermissions.NONE] is returned.
 *
 * This function is intentionally suspend so that future authentication methods (e.g. a
 * user-scoped JWT) can introduce truly asynchronous permission loaders without requiring
 * call-site changes.
 *
 * @return The resolved [UserPermissions] for this request.
 */
suspend fun ApplicationCall.getUserPermissions(): UserPermissions {
    attributes.getOrNull(USER_PERMISSIONS_KEY)?.let { return it }

    val session = getUserSession()
    if (session == null) {
        return UserPermissions.NONE.also { attributes.put(USER_PERMISSIONS_KEY, it) }
    }

    val userId = try {
        UUID.fromString(session.userId)
    } catch (_: IllegalArgumentException) {
        null
    }

    if (userId == null) {
        return UserPermissions.NONE.also { attributes.put(USER_PERMISSIONS_KEY, it) }
    }

    val userService = application.attributes[USER_SERVICE_KEY]
    val user = userService.findById(userId)

    val permissions = if (user == null) {
        UserPermissions.NONE
    } else {
        val isAdmin = user.roles.contains(UserRoles.ADMIN)
        UserPermissions(
            isAdmin = isAdmin,
            canCreateProject = isAdmin || user.roles.contains(UserRoles.CREATE_PROJECT)
        )
    }

    attributes.put(USER_PERMISSIONS_KEY, permissions)
    return permissions
}
