package com.mdeo.backend.service

import com.mdeo.common.model.ExecutionState
import com.mdeo.backend.database.ExecutionsTable
import com.mdeo.backend.database.FilesTable
import com.mdeo.backend.database.FileChildrenTable
import com.mdeo.common.model.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.*
import java.util.Base64
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Service for managing files and directories within projects.
 *
 * @param services The injected services providing access to configuration and other services
 */
class FileService(services: InjectedServices) : BaseService(), InjectedServices by services {
    
    /**
     * Checks if a project is locked due to an execution in initializing state.
     *
     * @param projectId The UUID of the project
     * @return true if the project is locked, false otherwise
     */
    private fun isProjectLocked(projectId: UUID): Boolean {
        return ExecutionsTable.selectAll()
            .where {
                (ExecutionsTable.projectId eq projectId.toKotlinUuid()) and
                (ExecutionsTable.state eq ExecutionState.INITIALIZING)
            }
            .count() > 0
    }
    
    /**
     * Returns a failure result if the project is locked.
     *
     * @param projectId The UUID of the project
     * @return ApiResult.Failure if locked, null otherwise
     */
    private fun checkProjectLock(projectId: UUID): ApiResult.Failure? {
        return if (isProjectLocked(projectId)) {
            ApiResult.Failure(
                ApiError(
                    ErrorCodes.PROJECT_LOCKED,
                    "Project is locked: an execution is initializing. File modifications are not allowed."
                )
            )
        } else {
            null
        }
    }
    
    /**
     * Reads the contents of a file.
     *
     * @param projectId The UUID of the project
     * @param path The path to the file
     * @return ApiResult containing the file contents as a byte array, or an error
     */
    fun readFile(projectId: UUID, path: String): ApiResult<ByteArray> {
        val normalizedPath = normalizePath(path)
        
        return transaction {
            val row = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }
                .firstOrNull()
            
            if (row == null) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_NOT_FOUND, "File not found: $path")
            }
            
            if (row[FilesTable.fileType] == FileType.DIRECTORY) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_IS_A_DIRECTORY, "Is a directory: $path")
            }
            
            val contentText = row[FilesTable.content] ?: ""
            val contentBytes = if (contentText.isEmpty()) ByteArray(0) else Base64.getDecoder().decode(contentText)
            success(contentBytes)
        }
    }
    
    /**
     * Writes content to a file, optionally creating or overwriting it.
     * Will fail if the project is locked due to an execution in initializing state.
     *
     * @param projectId The UUID of the project
     * @param path The path to the file
     * @param content The content to write as a byte array
     * @param create Whether to create the file if it doesn't exist
     * @param overwrite Whether to overwrite the file if it already exists
     * @return ApiResult indicating success or containing an error
     */
    fun writeFile(
        projectId: UUID, 
        path: String, 
        content: ByteArray, 
        create: Boolean, 
        overwrite: Boolean
    ): ApiResult<Unit> {
        val normalizedPath = normalizePath(path)
        val now = Instant.now()
        
        return transaction {
            checkProjectLock(projectId)?.let { return@transaction it }
            val existing = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }
                .firstOrNull()
            
            if (existing != null) {
                if (existing[FilesTable.fileType] == FileType.DIRECTORY) {
                    return@transaction fileSystemFailure(ErrorCodes.FILE_IS_A_DIRECTORY, "Is a directory: $path")
                }
                if (!overwrite) {
                    return@transaction fileSystemFailure(ErrorCodes.FILE_EXISTS, "File already exists: $path")
                }
                
                val currentVersion = existing[FilesTable.version]
                val contentText = Base64.getEncoder().encodeToString(content)
                FilesTable.update({ (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }) {
                    it[FilesTable.content] = contentText
                    it[version] = currentVersion + 1
                    it[updatedAt] = now
                }
            } else {
                if (!create) {
                    return@transaction fileSystemFailure(ErrorCodes.FILE_NOT_FOUND, "File not found: $path")
                }
                
                ensureParentDirectories(projectId, normalizedPath, now)
                
                val parentPath = getParentPath(normalizedPath)
                val basename = getBasename(normalizedPath)
                if (parentPath != null) {
                    addChildToDirectory(projectId, parentPath, basename, FileType.FILE, now)
                }
                
                val contentText = Base64.getEncoder().encodeToString(content)
                FilesTable.insert {
                    it[FilesTable.projectId] = projectId.toKotlinUuid()
                    it[FilesTable.path] = normalizedPath
                    it[fileType] = FileType.FILE
                    it[FilesTable.content] = contentText
                    it[version] = 1
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            
            success(Unit)
        }
    }
    
    /**
     * Creates a directory at the specified path.
     * Will fail if the project is locked due to an execution in initializing state.
     *
     * @param projectId The UUID of the project
     * @param path The path where the directory should be created
     * @return ApiResult indicating success or containing an error
     */
    fun mkdir(projectId: UUID, path: String): ApiResult<Unit> {
        val normalizedPath = normalizePath(path)
        val now = Instant.now()
        
        return transaction {
            checkProjectLock(projectId)?.let { return@transaction it }
            
            val existing = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }
                .firstOrNull()
            
            if (existing != null) {
                if (existing[FilesTable.fileType] == FileType.DIRECTORY) {
                    return@transaction success(Unit)
                }
                return@transaction fileSystemFailure(ErrorCodes.FILE_EXISTS, "File already exists: $path")
            }
            
            ensureParentDirectories(projectId, normalizedPath, now)
            
            val parentPath = getParentPath(normalizedPath)
            val basename = getBasename(normalizedPath)
            if (parentPath != null) {
                addChildToDirectory(projectId, parentPath, basename, FileType.DIRECTORY, now)
            }
            
            FilesTable.insert {
                it[FilesTable.projectId] = projectId.toKotlinUuid()
                it[FilesTable.path] = normalizedPath
                it[fileType] = FileType.DIRECTORY
                it[content] = null
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            success(Unit)
        }
    }
    
    /**
     * Lists the contents of a directory.
     *
     * @param projectId The UUID of the project
     * @param path The path to the directory
     * @return ApiResult containing a list of file entries, or an error
     */
    fun readdir(projectId: UUID, path: String): ApiResult<List<FileEntry>> {
        val normalizedPath = normalizePath(path)
        
        return transaction {
            val row = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }
                .firstOrNull()
            
            if (row == null) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_NOT_FOUND, "Directory not found: $path")
            }
            
            if (row[FilesTable.fileType] != FileType.DIRECTORY) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_NOT_A_DIRECTORY, "Not a directory: $path")
            }
            
            val result = FileChildrenTable.selectAll()
                .where { (FileChildrenTable.projectId eq projectId.toKotlinUuid()) and (FileChildrenTable.parentPath eq normalizedPath) }
                .map { childRow ->
                    FileEntry(childRow[FileChildrenTable.childName], childRow[FileChildrenTable.childType])
                }
            
            success(result)
        }
    }
    
    /**
     * Gets the file type (file or directory) of a path.
     *
     * @param projectId The UUID of the project
     * @param path The path to check
     * @return ApiResult containing the file type as an integer or null if not found, or an error
     */
    fun stat(projectId: UUID, path: String): ApiResult<Int?> {
        val normalizedPath = normalizePath(path)
        
        return transaction {
            val row = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }
                .firstOrNull()
            
            if (row == null) {
                success(null)
            } else {
                success(row[FilesTable.fileType])
            }
        }
    }
    
    /**
     * Gets the version of a file.
     *
     * @param projectId The UUID of the project
     * @param path The path to the file
     * @return ApiResult containing the file version as an integer, or an error
     */
    fun getFileVersion(projectId: UUID, path: String): ApiResult<Int> {
        val normalizedPath = normalizePath(path)
        
        return transaction {
            val row = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }
                .firstOrNull()
            
            if (row == null) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_NOT_FOUND, "File not found: $path")
            }
            
            if (row[FilesTable.fileType] == FileType.DIRECTORY) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_IS_A_DIRECTORY, "Is a directory: $path")
            }
            
            success(row[FilesTable.version])
        }
    }
    
    /**
     * Deletes a file or directory.
     * Will fail if the project is locked due to an execution in initializing state.
     *
     * @param projectId The UUID of the project
     * @param path The path to delete
     * @param recursive Whether to recursively delete directory contents
     * @return ApiResult indicating success or containing an error
     */
    fun delete(projectId: UUID, path: String, recursive: Boolean): ApiResult<Unit> {
        val normalizedPath = normalizePath(path)
        
        return transaction {
            checkProjectLock(projectId)?.let { return@transaction it }
            
            val row = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }
                .firstOrNull()
            
            if (row == null) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_NOT_FOUND, "File or directory not found: $path")
            }
            
            if (row[FilesTable.fileType] == FileType.DIRECTORY) {
                val childrenCount = FileChildrenTable.selectAll()
                    .where { (FileChildrenTable.projectId eq projectId.toKotlinUuid()) and (FileChildrenTable.parentPath eq normalizedPath) }
                    .count()
                
                if (childrenCount > 0 && !recursive) {
                    return@transaction fileSystemFailure(ErrorCodes.DIRECTORY_NOT_EMPTY, "Directory not empty: $path")
                }
                
                if (recursive) {
                    deleteRecursive(projectId, normalizedPath)
                }
            }
            
            val parentPath = getParentPath(normalizedPath)
            if (parentPath != null) {
                removeChildFromDirectory(projectId, parentPath, getBasename(normalizedPath))
            }
            
            FilesTable.deleteWhere { 
                (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) 
            }
            
            success(Unit)
        }
    }
    
    /**
     * Renames or moves a file or directory.
     * Will fail if the project is locked due to an execution in initializing state.
     *
     * @param projectId The UUID of the project
     * @param from The current path
     * @param to The new path
     * @param overwrite Whether to overwrite the destination if it exists
     * @return ApiResult indicating success or containing an error
     */
    fun rename(projectId: UUID, from: String, to: String, overwrite: Boolean): ApiResult<Unit> {
        val normalizedFrom = normalizePath(from)
        val normalizedTo = normalizePath(to)
        val now = Instant.now()
        
        return transaction {
            checkProjectLock(projectId)?.let { return@transaction it }
            
            val sourceRow = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedFrom) }
                .firstOrNull()
            
            if (sourceRow == null) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_NOT_FOUND, "Source not found: $from")
            }
            
            val destExists = FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedTo) }
                .count() > 0
            
            if (destExists && !overwrite) {
                return@transaction fileSystemFailure(ErrorCodes.FILE_EXISTS, "Destination already exists: $to")
            }
            
            if (destExists) {
                FilesTable.deleteWhere { 
                    (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedTo) 
                }
            }
            
            ensureParentDirectories(projectId, normalizedTo, now)
            
            val oldParent = getParentPath(normalizedFrom)
            val newParent = getParentPath(normalizedTo)
            
            if (oldParent != null) {
                removeChildFromDirectory(projectId, oldParent, getBasename(normalizedFrom))
            }
            if (newParent != null) {
                addChildToDirectory(projectId, newParent, getBasename(normalizedTo), sourceRow[FilesTable.fileType], now)
            }
            
            if (sourceRow[FilesTable.fileType] == FileType.DIRECTORY) {
                renameDirectoryChildren(projectId, normalizedFrom, normalizedTo)
            }
            
            FilesTable.update({ 
                (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedFrom) 
            }) {
                it[path] = normalizedTo
                it[updatedAt] = now
            }
            
            success(Unit)
        }
    }
    
    /**
     * Gets the parent path of a given path.
     *
     * @param path The input path
     * @return The parent path, or null if the input is empty
     */
    private fun getParentPath(path: String): String? {
        if (path.isEmpty()) return null
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash == -1) "" else path.substring(0, lastSlash)
    }
    
    /**
     * Gets the basename (last segment) of a path.
     *
     * @param path The input path
     * @return The basename of the path
     */
    private fun getBasename(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash == -1) path else path.substring(lastSlash + 1)
    }
    
    /**
     * Ensures that all parent directories exist for a given path.
     *
     * @param projectId The UUID of the project
     * @param path The path whose parent directories should be created
     * @param now The timestamp to use for creation
     */
    private fun ensureParentDirectories(projectId: UUID, path: String, now: Instant) {
        val parentPath = getParentPath(path) ?: return
        
        val parent = FilesTable.selectAll()
            .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq parentPath) }
            .firstOrNull()
        
        if (parent == null) {
            ensureParentDirectories(projectId, parentPath, now)
            
            val grandparentPath = getParentPath(parentPath)
            if (grandparentPath != null) {
                addChildToDirectory(projectId, grandparentPath, getBasename(parentPath), FileType.DIRECTORY, now)
            }
            
            FilesTable.insert {
                it[FilesTable.projectId] = projectId.toKotlinUuid()
                it[FilesTable.path] = parentPath
                it[fileType] = FileType.DIRECTORY
                it[content] = null
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }
    
    /**
     * Adds a child entry to a directory's children list.
     *
     * @param projectId The UUID of the project
     * @param dirPath The path to the directory
     * @param childName The name of the child to add
     * @param childType The type of the child (file or directory)
     * @param now The timestamp to use for the update
     */
    private fun addChildToDirectory(projectId: UUID, dirPath: String, childName: String, childType: Int, now: Instant) {
        val existingChild = FileChildrenTable.selectAll()
            .where { 
                (FileChildrenTable.projectId eq projectId.toKotlinUuid()) and 
                (FileChildrenTable.parentPath eq dirPath) and 
                (FileChildrenTable.childName eq childName)
            }
            .firstOrNull()
        
        if (existingChild == null) {
            FileChildrenTable.insert {
                it[FileChildrenTable.projectId] = projectId.toKotlinUuid()
                it[FileChildrenTable.parentPath] = dirPath
                it[FileChildrenTable.childName] = childName
                it[FileChildrenTable.childType] = childType
            }
        }
    }
    
    /**
     * Removes a child entry from a directory's children list.
     *
     * @param projectId The UUID of the project
     * @param dirPath The path to the directory
     * @param childName The name of the child to remove
     */
    private fun removeChildFromDirectory(projectId: UUID, dirPath: String, childName: String) {
        FileChildrenTable.deleteWhere {
            (FileChildrenTable.projectId eq projectId.toKotlinUuid()) and
            (FileChildrenTable.parentPath eq dirPath) and
            (FileChildrenTable.childName eq childName)
        }
    }
    
    /**
     * Recursively deletes a directory and all its children.
     *
     * @param projectId The UUID of the project
     * @param path The path to the directory to delete
     */
    private fun deleteRecursive(projectId: UUID, path: String) {
        val row = FilesTable.selectAll()
            .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq path) }
            .firstOrNull() ?: return
        
        if (row[FilesTable.fileType] == FileType.DIRECTORY) {
            val children = FileChildrenTable.selectAll()
                .where { (FileChildrenTable.projectId eq projectId.toKotlinUuid()) and (FileChildrenTable.parentPath eq path) }
                .map { it[FileChildrenTable.childName] }
            
            for (childName in children) {
                val childPath = if (path.isEmpty()) childName else "$path/$childName"
                deleteRecursive(projectId, childPath)
                FilesTable.deleteWhere { 
                    (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq childPath) 
                }
            }
            
            FileChildrenTable.deleteWhere {
                (FileChildrenTable.projectId eq projectId.toKotlinUuid()) and (FileChildrenTable.parentPath eq path)
            }
        }
    }
    
    /**
     * Renames all children of a directory when the directory is renamed.
     *
     * @param projectId The UUID of the project
     * @param oldPath The old directory path
     * @param newPath The new directory path
     */
    private fun renameDirectoryChildren(projectId: UUID, oldPath: String, newPath: String) {
        val prefix = if (oldPath.isEmpty()) "" else "$oldPath/"
        val newPrefix = if (newPath.isEmpty()) "" else "$newPath/"
        
        FilesTable.selectAll()
            .where { 
                (FilesTable.projectId eq projectId.toKotlinUuid()) and 
                (FilesTable.path like "$prefix%") 
            }
            .forEach { row ->
                val oldChildPath = row[FilesTable.path]
                val newChildPath = newPrefix + oldChildPath.substring(prefix.length)
                
                FilesTable.update({ 
                    (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq oldChildPath) 
                }) {
                    it[path] = newChildPath
                    it[updatedAt] = Instant.now()
                }
            }
        
        FileChildrenTable.selectAll()
            .where {
                (FileChildrenTable.projectId eq projectId.toKotlinUuid()) and
                (FileChildrenTable.parentPath like "$prefix%")
            }
            .forEach { row ->
                val oldParentPath = row[FileChildrenTable.parentPath]
                val newParentPath = newPrefix + oldParentPath.substring(prefix.length)
                
                FileChildrenTable.update({
                    (FileChildrenTable.projectId eq projectId.toKotlinUuid()) and
                    (FileChildrenTable.parentPath eq oldParentPath) and
                    (FileChildrenTable.childName eq row[FileChildrenTable.childName])
                }) {
                    it[parentPath] = newParentPath
                }
            }
    }
}
