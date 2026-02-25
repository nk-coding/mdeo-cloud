package com.mdeo.backend.plugins

import kotlinx.serialization.Serializable

/**
 * Session data stored in a signed cookie containing essential user information.
 *
 * @property userId Unique identifier for the user
 * @property username The user's username
 * @property isAdmin Whether the user has administrator privileges
 * @property canCreateProject Whether the user can create projects globally
 */
@Serializable
data class UserSession(
    val userId: String,
    val username: String,
    val isAdmin: Boolean,
    val canCreateProject: Boolean
)
