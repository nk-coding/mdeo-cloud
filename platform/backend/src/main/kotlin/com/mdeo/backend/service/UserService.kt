package com.mdeo.backend.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.mdeo.backend.database.UsersTable
import com.mdeo.common.model.User
import com.mdeo.common.model.UserInfo
import com.mdeo.common.model.UserRoles
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Service for managing user accounts and authentication.
 *
 * @param services The injected services providing access to configuration and other services
 */
class UserService(services: InjectedServices) : BaseService(), InjectedServices by services {
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    /**
     * Creates a default admin user if it does not already exist.
     *
     * @param username The username for the admin account
     * @param password The password for the admin account
     */
    suspend fun createDefaultAdmin(username: String, password: String) {
        transaction {
            val existing = UsersTable.selectAll()
                .where { UsersTable.username eq username }
                .firstOrNull()
            
            if (existing == null) {
                val now = Instant.now()
                val passwordHash = hashPassword(password)
                
                UsersTable.insert {
                    it[id] = UUID.randomUUID().toKotlinUuid()
                    it[UsersTable.username] = username
                    it[UsersTable.passwordHash] = passwordHash
                    it[roles] = setOf(UserRoles.USER, UserRoles.ADMIN).joinToString(",")
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                
                logger.info("Created default admin user: $username")
            } else {
                logger.info("Admin user already exists: $username")
            }
        }
    }
    
    /**
     * Finds a user by their username.
     *
     * @param username The username to search for
     * @return The user if found, null otherwise
     */
    fun findByUsername(username: String): User? {
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.username eq username }
                .firstOrNull()
                ?.toUser()
        }
    }
    
    /**
     * Finds a user by their unique identifier.
     *
     * @param userId The UUID of the user to find
     * @return The user if found, null otherwise
     */
    fun findById(userId: UUID): User? {
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.id eq userId.toKotlinUuid() }
                .firstOrNull()
                ?.toUser()
        }
    }
    
    /**
     * Finds basic user information by user ID.
     *
     * @param userId The UUID of the user
     * @return The user information if found, null otherwise
     */
    fun findUserInfoById(userId: UUID): UserInfo? {
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.id eq userId.toKotlinUuid() }
                .firstOrNull()
                ?.let { UserInfo(it[UsersTable.id].toJavaUuid().toString(), it[UsersTable.username]) }
        }
    }

    /**
     * Creates a new user account if the username is not already taken.
     *
     * @param username The desired username
     * @param password The password to store (will be hashed)
     * @return The created user, or null if the username already exists
     */
    fun createUser(username: String, password: String): User? {
        return transaction {
            val existing = UsersTable.selectAll()
                .where { UsersTable.username eq username }
                .firstOrNull()

            if (existing != null) {
                return@transaction null
            }

            val now = Instant.now()
            val userId = UUID.randomUUID()
            val passwordHash = hashPassword(password)
            val roles = mutableSetOf(UserRoles.USER)
            if (config.defaultNewUserCanCreateProject) {
                roles.add(UserRoles.CREATE_PROJECT)
            }

            UsersTable.insert {
                it[id] = userId.toKotlinUuid()
                it[UsersTable.username] = username
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.roles] = roles.joinToString(",")
                it[createdAt] = now
                it[updatedAt] = now
            }

            UsersTable.selectAll()
                .where { UsersTable.id eq userId.toKotlinUuid() }
                .first()
                .toUser()
        }
    }
    
    /**
     * Verifies a user's password and returns the user if authentication succeeds.
     *
     * @param username The username to authenticate
     * @param password The password to verify
     * @return The user if authentication succeeds, null otherwise
     */
    fun verifyPassword(username: String, password: String): User? {
        return transaction {
            val row = UsersTable.selectAll()
                .where { UsersTable.username eq username }
                .firstOrNull() ?: return@transaction null
            
            val storedHash = row[UsersTable.passwordHash]
            val result = BCrypt.verifyer().verify(password.toCharArray(), storedHash)
            
            if (result.verified) {
                row.toUser()
            } else {
                null
            }
        }
    }
    
    /**
     * Changes a user's password after verifying the current password.
     *
     * @param userId The UUID of the user
     * @param currentPassword The current password for verification
     * @param newPassword The new password to set
     * @return true if the password was successfully changed, false otherwise
     */
    fun changePassword(userId: UUID, currentPassword: String, newPassword: String): Boolean {
        return transaction {
            val row = UsersTable.selectAll()
                .where { UsersTable.id eq userId.toKotlinUuid() }
                .firstOrNull() ?: return@transaction false
            
            val storedHash = row[UsersTable.passwordHash]
            val result = BCrypt.verifyer().verify(currentPassword.toCharArray(), storedHash)
            
            if (!result.verified) {
                return@transaction false
            }
            
            val newHash = hashPassword(newPassword)
            UsersTable.update({ UsersTable.id eq userId.toKotlinUuid() }) {
                it[passwordHash] = newHash
                it[updatedAt] = Instant.now()
            }
            
            true
        }
    }
    
    /**
     * Changes a user's password without requiring the current password (admin only).
     *
     * @param userId The UUID of the user whose password should be changed
     * @param newPassword The new password to set
     * @return true if the password was successfully changed, false if user not found
     */
    fun adminChangePassword(userId: UUID, newPassword: String): Boolean {
        return transaction {
            val exists = UsersTable.selectAll()
                .where { UsersTable.id eq userId.toKotlinUuid() }
                .count() > 0
            
            if (!exists) {
                return@transaction false
            }
            
            val newHash = hashPassword(newPassword)
            UsersTable.update({ UsersTable.id eq userId.toKotlinUuid() }) {
                it[passwordHash] = newHash
                it[updatedAt] = Instant.now()
            }
            
            true
        }
    }
    
    /**
     * Retrieves all users in the system.
     *
     * @return List of all users
     */
    fun getAllUsers(): List<UserInfo> {
        return transaction {
            UsersTable.selectAll()
                .map {
                    val roles = parseRoles(it[UsersTable.roles])
                    UserInfo(
                        id = it[UsersTable.id].toJavaUuid().toString(),
                        username = it[UsersTable.username],
                        isAdmin = roles.contains(UserRoles.ADMIN),
                        canCreateProject = roles.contains(UserRoles.ADMIN) || roles.contains(UserRoles.CREATE_PROJECT)
                    )
                }
        }
    }

    /**
     * Updates a user's global permissions.
     *
     * @param userId The UUID of the user to update
     * @param isAdmin Whether the user should have global administrator privileges
     * @param canCreateProject Whether the user should have create-project permission
     * @return Result indicating success or the validation failure reason
     */
    fun updateUserGlobalPermissions(
        userId: UUID,
        isAdmin: Boolean,
        canCreateProject: Boolean
    ): UpdateUserPermissionsResult {
        return transaction {
            val row = UsersTable.selectAll()
                .where { UsersTable.id eq userId.toKotlinUuid() }
                .firstOrNull() ?: return@transaction UpdateUserPermissionsResult.USER_NOT_FOUND

            val currentRoles = parseRoles(row[UsersTable.roles]).toMutableSet()
            val currentlyAdmin = currentRoles.contains(UserRoles.ADMIN)

            if (currentlyAdmin && !isAdmin && countGlobalAdmins() <= 1) {
                return@transaction UpdateUserPermissionsResult.LAST_GLOBAL_ADMIN
            }

            currentRoles.add(UserRoles.USER)
            if (isAdmin) {
                currentRoles.add(UserRoles.ADMIN)
                currentRoles.add(UserRoles.CREATE_PROJECT)
            } else {
                currentRoles.remove(UserRoles.ADMIN)
                if (canCreateProject) {
                    currentRoles.add(UserRoles.CREATE_PROJECT)
                } else {
                    currentRoles.remove(UserRoles.CREATE_PROJECT)
                }
            }

            UsersTable.update({ UsersTable.id eq userId.toKotlinUuid() }) {
                it[roles] = currentRoles.joinToString(",")
                it[updatedAt] = Instant.now()
            }

            UpdateUserPermissionsResult.SUCCESS
        }
    }

    private fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    private fun countGlobalAdmins(): Int {
        return UsersTable.selectAll()
            .count {
                parseRoles(it[UsersTable.roles]).contains(UserRoles.ADMIN)
            }
    }

    private fun parseRoles(rawRoles: String): Set<String> {
        return rawRoles
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun ResultRow.toUser(): User {
        return User(
            id = this[UsersTable.id].toJavaUuid().toString(),
            username = this[UsersTable.username],
            roles = parseRoles(this[UsersTable.roles]).toList()
        )
    }
}

enum class UpdateUserPermissionsResult {
    SUCCESS,
    USER_NOT_FOUND,
    LAST_GLOBAL_ADMIN
}
