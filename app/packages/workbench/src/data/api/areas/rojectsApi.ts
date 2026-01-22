import type { ApiResult, ProjectError } from "../apiResult";
import type { Project } from "../../project/project";
import type { BackendApiCore } from "../backendApi";

/**
 * User info for project owner listings
 */
export interface UserInfo {
    id: string;
    username: string;
}

/**
 * API for project management operations.
 * Provides methods for creating, updating, deleting projects,
 * and managing project ownership.
 */
export class ProjectsApi {
    /**
     * Creates a new ProjectsApi instance
     *
     * @param core The core backend API providing HTTP utilities
     */
    constructor(private readonly core: BackendApiCore) {}

    /**
     * Gets all projects accessible to the current user
     *
     * @returns A promise resolving to an array of all accessible projects
     */
    async getAll(): Promise<ApiResult<Project[], ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects`);
    }

    /**
     * Creates a new project with the specified name
     *
     * @param name The name for the new project
     * @returns A promise resolving to the created project
     */
    async create(name: string): Promise<ApiResult<Project, ProjectError>> {
        return this.core.fetchApiResult<Project>(`${this.core.baseUrl}/projects`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name })
        });
    }

    /**
     * Updates an existing project
     *
     * @param projectId The ID of the project to update
     * @param updates The fields to update (currently only name)
     * @returns A promise resolving to the updated project
     */
    async update(projectId: string, updates: { name?: string }): Promise<ApiResult<Project, ProjectError>> {
        return this.core.fetchApiResult<Project>(`${this.core.baseUrl}/projects/${projectId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(updates)
        });
    }

    /**
     * Deletes a project and all its associated files
     *
     * @param projectId The ID of the project to delete
     * @returns A promise resolving to success or an error
     */
    async delete(projectId: string): Promise<ApiResult<void, ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}`, {
            method: "DELETE"
        });
    }

    /**
     * Gets all owners of a project
     *
     * @param projectId The ID of the project
     * @returns A promise resolving to an array of user info for each owner
     */
    async getOwners(projectId: string): Promise<ApiResult<UserInfo[], ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/owners`);
    }

    /**
     * Adds a user as an owner of a project
     *
     * @param projectId The ID of the project
     * @param userId The ID of the user to add as owner
     * @returns A promise resolving to success or an error
     */
    async addOwner(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/owners`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId })
        });
    }

    /**
     * Removes a user as an owner from a project
     *
     * @param projectId The ID of the project
     * @param userId The ID of the user to remove as owner
     * @returns A promise resolving to success or an error
     */
    async removeOwner(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/owners/${userId}`, {
            method: "DELETE"
        });
    }
}
