package com.mdeo.common.model

import kotlinx.serialization.Serializable

/**
 * Request payload for user login.
 *
 * @property username Username for authentication
 * @property password Password for authentication
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Request payload for user registration.
 *
 * @property username Desired username
 * @property password Desired password
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)

/**
 * Response payload for successful user login.
 *
 * @property user The authenticated user's information
 */
@Serializable
data class LoginResponse(
    val user: UserInfo
)

/**
 * Request payload for a user to change their own password.
 *
 * @property currentPassword The user's current password for verification
 * @property newPassword The new password to set
 */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

/**
 * Request payload for an administrator to change another user's password.
 *
 * @property newPassword The new password to set for the target user
 */
@Serializable
data class AdminChangePasswordRequest(
    val newPassword: String
)
