package com.mdeo.common.model

import kotlinx.serialization.Serializable

/**
 * Represents a user in the system.
 *
 * @property id Unique identifier for the user
 * @property username Username for authentication and display
 * @property roles List of role names assigned to the user
 */
@Serializable
data class User(
    val id: String,
    val username: String,
    val roles: List<String>
)

/**
 * Represents basic user information for display purposes.
 *
 * @property id Unique identifier for the user
 * @property username Username for authentication and display
 * @property isAdmin Flag indicating whether the user has administrative privileges
 */
@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val isAdmin: Boolean = false
)

/**
 * Constants defining available user roles in the system.
 */
object UserRoles {
    const val USER = "user"
    const val ADMIN = "admin"
}
