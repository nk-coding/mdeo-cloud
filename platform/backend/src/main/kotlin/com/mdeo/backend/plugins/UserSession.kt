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
 * @property createdAt Epoch-seconds timestamp of when this session was first created.
 *   Used to enforce an absolute session lifetime (sliding extension is capped at this
 *   plus [com.mdeo.backend.config.SessionConfig.maxAbsoluteSeconds]).
 */
@Serializable
data class UserSession(
    val userId: String,
    val username: String,
    val createdAt: Long
)
