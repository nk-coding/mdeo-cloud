import type { ApiResult, ProjectError } from "../apiResult";
import type { Project } from "../../project/project";
import type { BackendApiCore } from "../backendApi";

/**
 * User info for project user listings
 */
export interface ProjectUserInfo {
    id: string;
    username: string;
    isAdmin: boolean;
    canExecute: boolean;
    canWrite: boolean;
}

/**
 * Project membership info for a specific user
 */
export interface UserProjectMembership {
    projectId: string;
    projectName: string;
    isAdmin: boolean;
    canExecute: boolean;
    canWrite: boolean;
}

/**
 * API for project management operations.
 * Provides methods for creating, updating, deleting projects,
 * and managing project users.
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
     * Gets all users of a project
     *
     * @param projectId The ID of the project
     * @returns A promise resolving to an array of user info for each project user
     */
    async getUsers(projectId: string): Promise<ApiResult<ProjectUserInfo[], ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/users`);
    }

    /**
     * Adds a user to a project with permissions
     *
     * @param projectId The ID of the project
     * @param userId The ID of the user to add
     * @param isAdmin Whether the user should be a project admin
     * @param canExecute Whether the user should have execute permission
     * @param canWrite Whether the user should have write permission
     * @returns A promise resolving to success or an error
     */
    async addUser(
        projectId: string,
        userId: string,
        isAdmin = false,
        canExecute = false,
        canWrite = false
    ): Promise<ApiResult<void, ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/users`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId, isAdmin, canExecute, canWrite })
        });
    }

    /**
     * Updates project permissions for a user
     *
     * @param projectId The ID of the project
     * @param userId The ID of the user to update
     * @param isAdmin Whether the user should be a project admin
     * @param canExecute Whether the user should have execute permission
     * @param canWrite Whether the user should have write permission
     * @returns A promise resolving to success or an error
     */
    async updateUserPermissions(
        projectId: string,
        userId: string,
        isAdmin: boolean,
        canExecute: boolean,
        canWrite: boolean
    ): Promise<ApiResult<void, ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/users/${userId}/permissions`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ isAdmin, canExecute, canWrite })
        });
    }

    /**
     * Removes a user from a project
     *
     * @param projectId The ID of the project
     * @param userId The ID of the user to remove
     * @returns A promise resolving to success or an error
     */
    async removeUser(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/users/${userId}`, {
            method: "DELETE"
        });
    }

    async getOwners(projectId: string): Promise<ApiResult<ProjectUserInfo[], ProjectError>> {
        return this.getUsers(projectId);
    }

    async addOwner(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>> {
        return this.addUser(projectId, userId, true, true, true);
    }

    async removeOwner(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>> {
        return this.removeUser(projectId, userId);
    }
}
