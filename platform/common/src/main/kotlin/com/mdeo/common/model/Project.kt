package com.mdeo.common.model

import kotlinx.serialization.Serializable

/**
 * Represents a project in the system.
 *
 * @property id Unique identifier for the project
 * @property name Display name of the project
 */
@Serializable
data class Project(
    val id: String,
    val name: String
)

/**
 * Request payload for creating a new project.
 *
 * @property name The name to assign to the new project
 */
@Serializable
data class CreateProjectRequest(
    val name: String
)

/**
 * Request payload for updating an existing project.
 *
 * @property name Optional new name for the project
 */
@Serializable
data class UpdateProjectRequest(
    val name: String? = null
)

/**
 * Request payload for adding an owner to a project.
 *
 * @property userId Unique identifier of the user to add as an owner
 */
@Serializable
data class AddOwnerRequest(
    val userId: String
)

/**
 * User entry in a project membership list.
 *
 * @property id Unique identifier of the user
 * @property username Username for display
 * @property isAdmin Whether the user has project admin permission
 * @property canExecute Whether the user has execution permission
 * @property canWrite Whether the user has write permission
 */
@Serializable
data class ProjectUserInfo(
    val id: String,
    val username: String,
    val isAdmin: Boolean,
    val canExecute: Boolean,
    val canWrite: Boolean
)

/**
 * Project membership entry grouped by project for user details.
 *
 * @property projectId Unique identifier of the project
 * @property projectName Name of the project
 * @property isAdmin Whether the user has project admin permission
 * @property canExecute Whether the user has execution permission
 * @property canWrite Whether the user has write permission
 */
@Serializable
data class UserProjectMembership(
    val projectId: String,
    val projectName: String,
    val isAdmin: Boolean,
    val canExecute: Boolean,
    val canWrite: Boolean
)

/**
 * Request payload for adding a user to a project with permissions.
 *
 * @property userId Unique identifier of the user to add
 * @property isAdmin Whether to grant project admin permission
 * @property canExecute Whether to grant execution permission
 * @property canWrite Whether to grant write permission
 */
@Serializable
data class AddProjectUserRequest(
    val userId: String,
    val isAdmin: Boolean = false,
    val canExecute: Boolean = false,
    val canWrite: Boolean = false
)

/**
 * Request payload for updating project permissions for a user.
 *
 * @property isAdmin Whether the user should have project admin permission
 * @property canExecute Whether the user should have execution permission
 * @property canWrite Whether the user should have write permission
 */
@Serializable
data class UpdateProjectUserPermissionsRequest(
    val isAdmin: Boolean,
    val canExecute: Boolean,
    val canWrite: Boolean
)
