package com.mdeo.backend.service

import com.mdeo.backend.database.*
import com.mdeo.common.model.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Service for managing projects and project ownership.
 * 
 * @param services The injected services providing access to configuration and other services
 */
class ProjectService(services: InjectedServices) : BaseService(), InjectedServices by services {
    
    /**
     * Retrieves all projects readable by a user.
     *
     * @param userId The UUID of the user
     * @param isGlobalAdmin Whether the user has global admin privileges
     * @return List of readable projects
     */
    fun getProjectsForUser(userId: UUID, isGlobalAdmin: Boolean): List<Project> {
        return transaction {
            if (isGlobalAdmin) {
                ProjectsTable.selectAll()
                    .map { it.toProject() }
            } else {
                (ProjectsTable innerJoin ProjectOwnersTable)
                    .selectAll()
                    .where { ProjectOwnersTable.userId eq userId.toKotlinUuid() }
                    .map { it.toProject() }
            }
        }
    }

    /**
     * Retrieves all project memberships for a specific user.
     *
     * @param userId The UUID of the user
     * @return List of project memberships for the user
     */
    fun getProjectMembershipsByUserId(userId: UUID): List<UserProjectMembership> {
        return transaction {
            (ProjectsTable innerJoin ProjectOwnersTable)
                .selectAll()
                .where { ProjectOwnersTable.userId eq userId.toKotlinUuid() }
                .map {
                    UserProjectMembership(
                        projectId = it[ProjectsTable.id].toJavaUuid().toString(),
                        projectName = it[ProjectsTable.name],
                        isAdmin = it[ProjectOwnersTable.isAdmin],
                        canExecute = it[ProjectOwnersTable.isAdmin] || it[ProjectOwnersTable.canExecute],
                        canWrite = it[ProjectOwnersTable.isAdmin] || it[ProjectOwnersTable.canWrite]
                    )
                }
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
                .where { ProjectsTable.id eq projectId.toKotlinUuid() }
                .firstOrNull()
                ?.toProject()
        }
    }
    
    /**
     * Checks whether a user has the specified permission for a project.
     *
     * @param projectId The UUID of the project
     * @param userId The UUID of the user
     * @param isGlobalAdmin Whether the user has global admin permission
     * @param permission The project permission to validate
     * @return true if the user has the permission, false otherwise
     */
    fun hasProjectPermission(
        projectId: UUID,
        userId: UUID,
        isGlobalAdmin: Boolean,
        permission: ProjectPermission
    ): Boolean {
        if (isGlobalAdmin) {
            return true
        }

        return transaction {
            val membership = ProjectOwnersTable.selectAll()
                .where { 
                    (ProjectOwnersTable.projectId eq projectId.toKotlinUuid()) and 
                    (ProjectOwnersTable.userId eq userId.toKotlinUuid()) 
                }
                .firstOrNull() ?: return@transaction false

            if (permission == ProjectPermission.READ) {
                return@transaction true
            }

            if (membership[ProjectOwnersTable.isAdmin]) {
                return@transaction true
            }

            when (permission) {
                ProjectPermission.ADMIN -> false
                ProjectPermission.EXECUTE -> membership[ProjectOwnersTable.canExecute]
                ProjectPermission.WRITE -> membership[ProjectOwnersTable.canWrite]
                ProjectPermission.READ -> true
            }
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
                it[id] = projectId.toKotlinUuid()
                it[ProjectsTable.name] = name
                it[createdAt] = now
                it[updatedAt] = now
            }

            ProjectOwnersTable.insert {
                it[ProjectOwnersTable.projectId] = projectId.toKotlinUuid()
                it[ProjectOwnersTable.userId] = creatorUserId.toKotlinUuid()
                it[ProjectOwnersTable.isAdmin] = true
                it[ProjectOwnersTable.canExecute] = true
                it[ProjectOwnersTable.canWrite] = true
            }

            val defaultPlugins = pluginService.getDefaultPlugins()
            for (pluginId in defaultPlugins) {
                ProjectPluginsTable.insert {
                    it[ProjectPluginsTable.projectId] = projectId.toKotlinUuid()
                    it[ProjectPluginsTable.pluginId] = pluginId.toKotlinUuid()
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
            val updated = ProjectsTable.update({ ProjectsTable.id eq projectId.toKotlinUuid() }) {
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
            val deleted = ProjectsTable.deleteWhere { ProjectsTable.id eq projectId.toKotlinUuid() }
            deleted > 0
        }
    }
    
    /**
     * Retrieves all users of a project with their project permissions.
     *
     * @param projectId The UUID of the project
     * @return List of user information for all project users
     */
    fun getProjectUsers(projectId: UUID): List<ProjectUserInfo> {
        return transaction {
            (ProjectOwnersTable innerJoin UsersTable)
                .selectAll()
                .where { ProjectOwnersTable.projectId eq projectId.toKotlinUuid() }
                .map {
                    ProjectUserInfo(
                        id = it[UsersTable.id].toJavaUuid().toString(),
                        username = it[UsersTable.username],
                        isAdmin = it[ProjectOwnersTable.isAdmin],
                        canExecute = it[ProjectOwnersTable.isAdmin] || it[ProjectOwnersTable.canExecute],
                        canWrite = it[ProjectOwnersTable.isAdmin] || it[ProjectOwnersTable.canWrite]
                    )
                }
        }
    }

    /**
     * Adds a user to a project with explicit permissions.
     *
     * @param projectId The UUID of the project
     * @param userId The UUID of the user to add
     * @param isAdmin Whether the user should be a project admin
     * @param canExecute Whether the user should have execute permission
     * @param canWrite Whether the user should have write permission
     * @return The result of the operation
     */
    fun addProjectUser(
        projectId: UUID,
        userId: UUID,
        isAdmin: Boolean,
        canExecute: Boolean,
        canWrite: Boolean
    ): AddProjectUserResult {
        return transaction {
            val projectExists = ProjectsTable.selectAll()
                .where { ProjectsTable.id eq projectId.toKotlinUuid() }
                .count() > 0
            
            if (!projectExists) {
                return@transaction AddProjectUserResult.PROJECT_NOT_FOUND
            }
            
            val userExists = UsersTable.selectAll()
                .where { UsersTable.id eq userId.toKotlinUuid() }
                .count() > 0
            
            if (!userExists) {
                return@transaction AddProjectUserResult.USER_NOT_FOUND
            }
            
            val alreadyMember = ProjectOwnersTable.selectAll()
                .where { 
                    (ProjectOwnersTable.projectId eq projectId.toKotlinUuid()) and 
                    (ProjectOwnersTable.userId eq userId.toKotlinUuid()) 
                }
                .count() > 0
            
            if (alreadyMember) {
                return@transaction AddProjectUserResult.ALREADY_MEMBER
            }
            
            ProjectOwnersTable.insert {
                it[ProjectOwnersTable.projectId] = projectId.toKotlinUuid()
                it[ProjectOwnersTable.userId] = userId.toKotlinUuid()
                it[ProjectOwnersTable.isAdmin] = isAdmin
                it[ProjectOwnersTable.canExecute] = isAdmin || canExecute
                it[ProjectOwnersTable.canWrite] = isAdmin || canWrite
            }
            
            AddProjectUserResult.SUCCESS
        }
    }

    /**
     * Updates project permissions of an existing user.
     *
     * @param projectId The UUID of the project
     * @param userId The UUID of the user
     * @param isAdmin Whether the user should be a project admin
     * @param canExecute Whether the user should have execute permission
     * @param canWrite Whether the user should have write permission
     * @return The result of the operation
     */
    fun updateProjectUserPermissions(
        projectId: UUID,
        userId: UUID,
        isAdmin: Boolean,
        canExecute: Boolean,
        canWrite: Boolean
    ): UpdateProjectUserPermissionsResult {
        return transaction {
            val projectExists = ProjectsTable.selectAll()
                .where { ProjectsTable.id eq projectId.toKotlinUuid() }
                .count() > 0
            if (!projectExists) {
                return@transaction UpdateProjectUserPermissionsResult.PROJECT_NOT_FOUND
            }

            val userExists = UsersTable.selectAll()
                .where { UsersTable.id eq userId.toKotlinUuid() }
                .count() > 0
            if (!userExists) {
                return@transaction UpdateProjectUserPermissionsResult.USER_NOT_FOUND
            }

            val membership = ProjectOwnersTable.selectAll()
                .where {
                    (ProjectOwnersTable.projectId eq projectId.toKotlinUuid()) and
                    (ProjectOwnersTable.userId eq userId.toKotlinUuid())
                }
                .firstOrNull() ?: return@transaction UpdateProjectUserPermissionsResult.NOT_MEMBER

            val currentlyAdmin = membership[ProjectOwnersTable.isAdmin]
            if (currentlyAdmin && !isAdmin && countProjectAdmins(projectId) <= 1) {
                return@transaction UpdateProjectUserPermissionsResult.LAST_PROJECT_ADMIN
            }

            ProjectOwnersTable.update({
                (ProjectOwnersTable.projectId eq projectId.toKotlinUuid()) and
                    (ProjectOwnersTable.userId eq userId.toKotlinUuid())
            }) {
                it[ProjectOwnersTable.isAdmin] = isAdmin
                it[ProjectOwnersTable.canExecute] = isAdmin || canExecute
                it[ProjectOwnersTable.canWrite] = isAdmin || canWrite
            }

            UpdateProjectUserPermissionsResult.SUCCESS
        }
    }

    /**
     * Removes a user from a project.
     * Cannot remove the last project admin.
     *
     * @param projectId The UUID of the project
     * @param userId The UUID of the user to remove
     * @return The result of the operation
     */
    fun removeProjectUser(projectId: UUID, userId: UUID): RemoveProjectUserResult {
        return transaction {
            val projectExists = ProjectsTable.selectAll()
                .where { ProjectsTable.id eq projectId.toKotlinUuid() }
                .count() > 0
            
            if (!projectExists) {
                return@transaction RemoveProjectUserResult.PROJECT_NOT_FOUND
            }
            
            val membership = ProjectOwnersTable.selectAll()
                .where { 
                    (ProjectOwnersTable.projectId eq projectId.toKotlinUuid()) and 
                    (ProjectOwnersTable.userId eq userId.toKotlinUuid()) 
                }
                .firstOrNull()
            
            if (membership == null) {
                return@transaction RemoveProjectUserResult.NOT_MEMBER
            }

            val isAdmin = membership[ProjectOwnersTable.isAdmin]
            if (isAdmin && countProjectAdmins(projectId) <= 1) {
                return@transaction RemoveProjectUserResult.LAST_PROJECT_ADMIN
            }
            
            ProjectOwnersTable.deleteWhere { 
                (ProjectOwnersTable.projectId eq projectId.toKotlinUuid()) and 
                (ProjectOwnersTable.userId eq userId.toKotlinUuid()) 
            }
            
            RemoveProjectUserResult.SUCCESS
        }
    }

    private fun countProjectAdmins(projectId: UUID): Long {
        return ProjectOwnersTable.selectAll()
            .where {
                (ProjectOwnersTable.projectId eq projectId.toKotlinUuid()) and
                    (ProjectOwnersTable.isAdmin eq true)
            }
            .count()
    }
    
    private fun ResultRow.toProject(): Project {
        return Project(
            id = this[ProjectsTable.id].toJavaUuid().toString(),
            name = this[ProjectsTable.name]
        )
    }
}

enum class AddProjectUserResult {
    SUCCESS,
    PROJECT_NOT_FOUND,
    USER_NOT_FOUND,
    ALREADY_MEMBER
}

enum class UpdateProjectUserPermissionsResult {
    SUCCESS,
    PROJECT_NOT_FOUND,
    USER_NOT_FOUND,
    NOT_MEMBER,
    LAST_PROJECT_ADMIN
}

enum class RemoveProjectUserResult {
    SUCCESS,
    PROJECT_NOT_FOUND,
    NOT_MEMBER,
    LAST_PROJECT_ADMIN
}

enum class ProjectPermission {
    ADMIN,
    EXECUTE,
    READ,
    WRITE
}
