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
