package com.mdeo.backend.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Users table schema for storing user accounts and authentication data.
 */
object UsersTable : Table("users") {
    val id = uuid("id")
    val username = varchar("username", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val roles = text("roles")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Projects table schema for storing project metadata.
 */
object ProjectsTable : Table("projects") {
    val id = uuid("id")
    val name = varchar("name", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Project owners table schema for managing project ownership relationships.
 */
object ProjectOwnersTable : Table("project_owners") {
    val projectId = uuid("project_id").references(ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    
    override val primaryKey = PrimaryKey(projectId, userId)
}

/**
 * Files table schema for storing project files and directory structures.
 */
object FilesTable : Table("files") {
    val projectId = uuid("project_id").references(ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 1024)
    val fileType = integer("file_type")
    val content = text("content").nullable()
    val version = integer("version").default(1)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(projectId, path)
}

/**
 * File children table schema for storing directory child relationships.
 */
object FileChildrenTable : Table("file_children") {
    val projectId = uuid("project_id").references(ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val parentPath = varchar("parent_path", 1024)
    val childName = varchar("child_name", 255)
    val childType = integer("child_type")
    
    override val primaryKey = PrimaryKey(projectId, parentPath, childName)
    
    init {
        index(false, projectId, parentPath)
    }
}

/**
 * File metadata table schema for storing additional metadata associated with files.
 */
object FileMetadataTable : Table("file_metadata") {
    val projectId = uuid("project_id").references(ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 1024)
    val metadata = json<JsonObject>("metadata", Json)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(projectId, path)
}

/**
 * Plugins table schema for storing registered plugins.
 */
object PluginsTable : Table("plugins") {
    val id = uuid("id")
    val url = varchar("url", 2048).uniqueIndex()
    val name = varchar("name", 255)
    val description = text("description")
    val icon = text("icon")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Project plugins table schema for managing plugin associations with projects.
 */
object ProjectPluginsTable : Table("project_plugins") {
    val projectId = uuid("project_id").references(ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val pluginId = uuid("plugin_id").references(PluginsTable.id, onDelete = ReferenceOption.CASCADE)
    
    override val primaryKey = PrimaryKey(projectId, pluginId)
}

/**
 * Language plugins table schema for storing language plugin data from plugin manifests.
 */
object LanguagePluginsTable : Table("language_plugins") {
    val id = varchar("id", 255)
    val pluginId = uuid("plugin_id").references(PluginsTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val extension = varchar("extension", 64)
    val defaultContent = text("default_content").nullable()
    val serverPluginImport = varchar("server_plugin_import", 2048)
    val editorPluginImport = varchar("editor_plugin_import", 2048).nullable()
    val editorStylesUrl = varchar("editor_styles_url", 2048).nullable()
    val languageConfiguration = text("language_configuration")
    val monarchTokensProvider = text("monarch_tokens_provider")
    val icon = text("icon")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(pluginId, id)
}

/**
 * Contribution plugins table schema for storing contribution plugins from plugin manifests.
 * Contribution plugins provide additional functionality to existing languages.
 */
object ContributionPluginsTable : Table("contribution_plugins") {
    val id = uuid("id")
    val pluginId = uuid("plugin_id").references(PluginsTable.id, onDelete = ReferenceOption.CASCADE)
    val languageId = varchar("language_id", 255)
    val description = text("description")
    val additionalKeywords = text("additional_keywords")
    val serverContributionPlugins = text("server_contribution_plugins")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * File data table schema for caching computed file data (e.g., AST).
 */
object FileDataTable : Table("file_data") {
    val projectId = uuid("project_id").references(ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 1024)
    val dataKey = varchar("data_key", 255)
    val data = json<JsonElement>("data", Json)
    val sourceVersion = integer("source_version")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(projectId, path, dataKey)
}

/**
 * File dependencies table schema for tracking file-to-file dependencies.
 */
object FileDependenciesTable : Table("file_dependencies") {
    val projectId = uuid("project_id").references(ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 1024)
    val dataKey = varchar("data_key", 255)
    val dependencyPath = varchar("dependency_path", 1024)
    val dependencyVersion = integer("dependency_version")
    
    init {
        index(true, projectId, path, dataKey, dependencyPath)
    }
}

/**
 * Data dependencies table schema for tracking file data-to-file data dependencies.
 */
object DataDependenciesTable : Table("data_dependencies") {
    val projectId = uuid("project_id").references(ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val path = varchar("path", 1024)
    val dataKey = varchar("data_key", 255)
    val dependencyPath = varchar("dependency_path", 1024)
    val dependencyKey = varchar("dependency_key", 255)
    val dependencyVersion = integer("dependency_version")
    
    init {
        index(true, projectId, path, dataKey, dependencyPath, dependencyKey)
    }
}
