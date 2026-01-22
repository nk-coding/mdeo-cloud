import type { ApiResult } from "../apiResult";
import type { CommonError, ProjectError } from "../apiResult";
import type { Project } from "../../project/project";
import type { BackendApiCore } from "../backendApi";
import type { User } from "./authApi";

/**
 * API for user management operations.
 * Provides methods for listing users, retrieving user projects,
 * and managing user admin status.
 */
export class UsersApi {
    /**
     * Creates a new UsersApi instance
     *
     * @param core The core backend API providing HTTP utilities
     */
    constructor(private readonly core: BackendApiCore) {}

    /**
     * Gets all users in the system
     *
     * @returns A promise resolving to an array of all users
     */
    async getAll(): Promise<ApiResult<User[], CommonError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/users`, {
            method: "GET"
        });
    }

    /**
     * Gets all projects owned by a specific user
     *
     * @param userId The ID of the user whose projects to retrieve
     * @returns A promise resolving to an array of projects owned by the user
     */
    async getProjects(userId: string): Promise<ApiResult<Project[], ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/users/${userId}/projects`, {
            method: "GET"
        });
    }

    /**
     * Updates a user's admin status
     *
     * @param userId The ID of the user to update
     * @param isAdmin The new admin status for the user
     * @returns A promise resolving to success or an error
     */
    async updateAdmin(userId: string, isAdmin: boolean): Promise<ApiResult<void, CommonError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/users/${userId}/admin`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ isAdmin })
        });
    }
}
