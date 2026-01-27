package com.mdeo.backend.service

import com.mdeo.backend.database.FileMetadataTable
import com.mdeo.common.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Service for managing file metadata within projects.
 *
 * @param services The injected services providing access to configuration and other services
 */
class MetadataService(services: InjectedServices) : BaseService(), InjectedServices by services {
    
    /**
     * Reads metadata for a file or directory.
     *
     * @param projectId The UUID of the project
     * @param path The path to the file or directory
     * @return ApiResult containing the metadata as a JsonObject, or an empty object if no metadata exists
     */
    fun readMetadata(projectId: UUID, path: String): ApiResult<JsonObject> {
        val normalizedPath = normalizePath(path)
        
        return transaction {
            val row = FileMetadataTable.selectAll()
                .where {
                    (FileMetadataTable.projectId eq projectId.toKotlinUuid()) and
                    (FileMetadataTable.path eq normalizedPath)
                }
                .firstOrNull()

            if (row == null) {
                return@transaction success(JsonObject(emptyMap()))
            }

            val metadata: JsonObject = row[FileMetadataTable.metadata]
            success(metadata)
        }
    }
    
    /**
     * Writes metadata for a file or directory.
     *
     * @param projectId The UUID of the project
     * @param path The path to the file or directory
     * @param metadata The metadata to write as a JsonObject
     * @return ApiResult indicating success or containing an error
     */
    fun writeMetadata(projectId: UUID, path: String, metadata: JsonObject): ApiResult<Unit> {
        val normalizedPath = normalizePath(path)
        val now = Instant.now()
        return transaction {
            val existing = FileMetadataTable.selectAll()
                .where { 
                    (FileMetadataTable.projectId eq projectId.toKotlinUuid()) and 
                    (FileMetadataTable.path eq normalizedPath) 
                }
                .firstOrNull()
            
            if (existing != null) {
                FileMetadataTable.update({
                    (FileMetadataTable.projectId eq projectId.toKotlinUuid()) and
                    (FileMetadataTable.path eq normalizedPath)
                }) {
                    it[FileMetadataTable.metadata] = metadata
                    it[updatedAt] = now
                }
            } else {
                FileMetadataTable.insert {
                    it[FileMetadataTable.projectId] = projectId.toKotlinUuid()
                    it[FileMetadataTable.path] = normalizedPath
                    it[FileMetadataTable.metadata] = metadata
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            
            success(Unit)
        }
    }
    
    /**
     * Deletes all metadata for a project.
     *
     * @param projectId The UUID of the project
     */
    fun deleteAllForProject(projectId: UUID) {
        transaction {
            FileMetadataTable.deleteWhere { FileMetadataTable.projectId eq projectId.toKotlinUuid() }
        }
    }
    
}
