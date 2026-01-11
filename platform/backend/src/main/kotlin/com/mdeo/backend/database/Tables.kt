package com.mdeo.backend.database

import org.jetbrains.exposed.sql.Table
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
    val projectId = uuid("project_id").references(ProjectsTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    
    override val primaryKey = PrimaryKey(projectId, userId)
}

/**
 * Files table schema for storing project files and directory structures.
 */
object FilesTable : Table("files") {
    val projectId = uuid("project_id").references(ProjectsTable.id)
    val path = varchar("path", 1024)
    val fileType = integer("file_type")
    val content = binary("content").nullable()
    val children = text("children").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(projectId, path)
}

/**
 * File metadata table schema for storing additional metadata associated with files.
 */
object FileMetadataTable : Table("file_metadata") {
    val projectId = uuid("project_id").references(ProjectsTable.id)
    val path = varchar("path", 1024)
    val metadata = text("metadata")
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
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Project plugins table schema for managing plugin associations with projects.
 */
object ProjectPluginsTable : Table("project_plugins") {
    val projectId = uuid("project_id").references(ProjectsTable.id)
    val pluginId = uuid("plugin_id").references(PluginsTable.id)
    
    override val primaryKey = PrimaryKey(projectId, pluginId)
}
