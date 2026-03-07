package com.mdeo.backend.plugins

import kotlinx.serialization.Serializable

/**
 * Session data stored in a signed cookie.
 *
 * Intentionally contains only stable identity information. Security-relevant
 * permissions ([isAdmin], [canCreateProject]) are **not** stored here; they are
 * resolved fresh from the database on each request via
 * [ApplicationCall.getUserPermissions] so that permission changes take effect
 * immediately without requiring the user to re-login.
 *
 * @property userId Unique identifier for the user.
 * @property username The user's display name.
 */
@Serializable
data class UserSession(
    val userId: String,
    val username: String
)
