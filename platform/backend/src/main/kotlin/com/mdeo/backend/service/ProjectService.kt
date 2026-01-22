package com.mdeo.backend.service

import com.mdeo.backend.database.*
import com.mdeo.common.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * Service for managing projects and project ownership.
 * 
 * @param services The injected services providing access to configuration and other services
 */
class ProjectService(services: InjectedServices) : BaseService(), InjectedServices by services {
    
    /**
     * Retrieves all projects accessible by a user.
     * Admins can see all projects, while regular users see only projects they own.
     *
     * @param userId The UUID of the user
     * @param isAdmin Whether the user has admin privileges
     * @return List of projects accessible by the user
     */
    fun getProjectsForUser(userId: UUID, isAdmin: Boolean): List<Project> {
        return transaction {
            if (isAdmin) {
                ProjectsTable.selectAll()
                    .map { it.toProject() }
            } else {
                (ProjectsTable innerJoin ProjectOwnersTable)
                    .selectAll()
                    .where { ProjectOwnersTable.userId eq userId }
                    .map { it.toProject() }
            }
        }
    }
    
    /**
     * Retrieves all projects owned by a specific user.
     *
     * @param userId The UUID of the user
     * @return List of projects owned by the user
     */
    fun getProjectsByUserId(userId: UUID): List<Project> {
        return transaction {
            (ProjectsTable innerJoin ProjectOwnersTable)
                .selectAll()
                .where { ProjectOwnersTable.userId eq userId }
                .map { it.toProject() }
        }
    }
    
    /**
     * Retrieves a specific project by its ID.
     *
     * @param projectId The UUID of the project
     * @return The project if found, null otherwise
     */
    fun getProject(projectId: UUID): Project? {
        return transaction {
            ProjectsTable.selectAll()
                .where { ProjectsTable.id eq projectId }
                .firstOrNull()
                ?.toProject()
        }
    }
    
    /**
     * Checks if a user is an owner of a project or has admin privileges.
     *
     * @param projectId The UUID of the project
     * @param userId The UUID of the user
     * @param isAdmin Whether the user has admin privileges
     * @return true if the user is an owner or admin, false otherwise
     */
    fun isOwnerOrAdmin(projectId: UUID, userId: UUID, isAdmin: Boolean): Boolean {
        if (isAdmin) return true
        
        return transaction {
            ProjectOwnersTable.selectAll()
                .where { 
                    (ProjectOwnersTable.projectId eq projectId) and 
                    (ProjectOwnersTable.userId eq userId) 
                }
                .count() > 0
        }
    }
    
    /**
     * Creates a new project with the creator as the initial owner.
     * Automatically adds default plugins to the new project.
     *
     * @param name The name of the project
     * @param creatorUserId The UUID of the user creating the project
     * @return The newly created project
     */
    fun createProject(name: String, creatorUserId: UUID): Project {
        val projectId = UUID.randomUUID()
        val now = Instant.now()

        transaction {
            ProjectsTable.insert {
                it[id] = projectId
                it[ProjectsTable.name] = name
                it[createdAt] = now
                it[updatedAt] = now
            }

            ProjectOwnersTable.insert {
                it[ProjectOwnersTable.projectId] = projectId
                it[userId] = creatorUserId
            }

            val defaultPlugins = pluginService.getDefaultPlugins()
            for (pluginId in defaultPlugins) {
                ProjectPluginsTable.insert {
                    it[ProjectPluginsTable.projectId] = projectId
                    it[ProjectPluginsTable.pluginId] = pluginId
                }
            }
        }

        fileService.mkdir(projectId, "")

        return Project(
            id = projectId.toString(),
            name = name
        )
    }
    
    /**
     * Updates a project's properties.
     *
     * @param projectId The UUID of the project to update
     * @param updates The update request containing new property values
     * @return true if the project was updated, false if not found
     */
    fun updateProject(projectId: UUID, updates: UpdateProjectRequest): Boolean {
        return transaction {
            val updated = ProjectsTable.update({ ProjectsTable.id eq projectId }) {
                updates.name?.let { name -> it[ProjectsTable.name] = name }
                it[updatedAt] = Instant.now()
            }
            updated > 0
        }
    }
    
    /**
     * Deletes a project and all its related data.
     * Cascading deletes automatically handle files, metadata, plugins, and owners.
     *
     * @param projectId The UUID of the project to delete
     * @return true if the project was deleted, false if not found
     */
    fun deleteProject(projectId: UUID): Boolean {
        return transaction {
            val deleted = ProjectsTable.deleteWhere { ProjectsTable.id eq projectId }
            deleted > 0
        }
    }
    
    /**
     * Retrieves all owners of a project.
     *
     * @param projectId The UUID of the project
     * @return List of user information for all project owners
     */
    fun getProjectOwners(projectId: UUID): List<UserInfo> {
        return transaction {
            (ProjectOwnersTable innerJoin UsersTable)
                .selectAll()
                .where { ProjectOwnersTable.projectId eq projectId }
                .map { 
                    UserInfo(
                        id = it[UsersTable.id].toString(),
                        username = it[UsersTable.username]
                    )
                }
        }
    }
    
    /**
     * Adds a user as an owner of a project.
     *
     * @param projectId The UUID of the project
     * @param userId The UUID of the user to add as owner
     * @return The result of the operation
     */
    fun addOwner(projectId: UUID, userId: UUID): AddOwnerResult {
        return transaction {
            val projectExists = ProjectsTable.selectAll()
                .where { ProjectsTable.id eq projectId }
                .count() > 0
            
            if (!projectExists) {
                return@transaction AddOwnerResult.PROJECT_NOT_FOUND
            }
            
            val userExists = UsersTable.selectAll()
                .where { UsersTable.id eq userId }
                .count() > 0
            
            if (!userExists) {
                return@transaction AddOwnerResult.USER_NOT_FOUND
            }
            
            val alreadyOwner = ProjectOwnersTable.selectAll()
                .where { 
                    (ProjectOwnersTable.projectId eq projectId) and 
                    (ProjectOwnersTable.userId eq userId) 
                }
                .count() > 0
            
            if (alreadyOwner) {
                return@transaction AddOwnerResult.ALREADY_OWNER
            }
            
            ProjectOwnersTable.insert {
                it[ProjectOwnersTable.projectId] = projectId
                it[ProjectOwnersTable.userId] = userId
            }
            
            AddOwnerResult.SUCCESS
        }
    }
    
    /**
     * Removes a user as an owner of a project.
     * Cannot remove the last owner of a project.
     *
     * @param projectId The UUID of the project
     * @param userId The UUID of the user to remove as owner
     * @return The result of the operation
     */
    fun removeOwner(projectId: UUID, userId: UUID): RemoveOwnerResult {
        return transaction {
            val projectExists = ProjectsTable.selectAll()
                .where { ProjectsTable.id eq projectId }
                .count() > 0
            
            if (!projectExists) {
                return@transaction RemoveOwnerResult.PROJECT_NOT_FOUND
            }
            
            val isOwner = ProjectOwnersTable.selectAll()
                .where { 
                    (ProjectOwnersTable.projectId eq projectId) and 
                    (ProjectOwnersTable.userId eq userId) 
                }
                .count() > 0
            
            if (!isOwner) {
                return@transaction RemoveOwnerResult.NOT_OWNER
            }
            
            val ownerCount = ProjectOwnersTable.selectAll()
                .where { ProjectOwnersTable.projectId eq projectId }
                .count()
            
            if (ownerCount <= 1) {
                return@transaction RemoveOwnerResult.LAST_OWNER
            }
            
            ProjectOwnersTable.deleteWhere { 
                (ProjectOwnersTable.projectId eq projectId) and 
                (ProjectOwnersTable.userId eq userId) 
            }
            
            RemoveOwnerResult.SUCCESS
        }
    }
    
    private fun ResultRow.toProject(): Project {
        return Project(
            id = this[ProjectsTable.id].toString(),
            name = this[ProjectsTable.name]
        )
    }
}

enum class AddOwnerResult {
    SUCCESS, PROJECT_NOT_FOUND, USER_NOT_FOUND, ALREADY_OWNER
}

enum class RemoveOwnerResult {
    SUCCESS, PROJECT_NOT_FOUND, NOT_OWNER, LAST_OWNER
}
