package com.mdeo.common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/**
 * Represents a backend plugin configuration.
 *
 * @property id Unique identifier for the plugin
 * @property url URL where the plugin is hosted or can be accessed
 * @property name Display name of the plugin
 * @property description Brief description of the plugin's functionality
 * @property icon Icon data in Lucide IconNode format
 */
@Serializable
data class BackendPlugin(
    val id: String,
    val url: String,
    val name: String,
    val description: String,
    val icon: JsonArray
)

/**
 * Represents a fully resolved plugin with all its metadata.
 *
 * @property id Unique identifier for the plugin
 * @property url URL where the plugin is hosted or can be accessed
 * @property name Display name of the plugin
 * @property description Brief description of the plugin's functionality
 * @property icon Icon data in Lucide IconNode format
 */
@Serializable
data class ResolvedPlugin(
    val id: String,
    val url: String,
    val name: String,
    val description: String,
    val icon: JsonArray
)

/**
 * Request payload for creating a new plugin.
 *
 * @property url URL of the plugin to be created
 */
@Serializable
data class CreatePluginRequest(
    val url: String
)

/**
 * Request payload for adding a plugin to a project.
 *
 * @property pluginId Unique identifier of the plugin to add to the project
 */
@Serializable
data class AddPluginToProjectRequest(
    val pluginId: String
)
