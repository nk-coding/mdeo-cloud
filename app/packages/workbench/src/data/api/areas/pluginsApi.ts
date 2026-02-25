import type { ApiResult, PluginError } from "../apiResult";
import type { Plugin } from "@mdeo/plugin";
import type { BackendApiCore } from "../backendApi";

/**
 * API for plugin management operations.
 * Provides methods for creating, deleting, and managing plugins,
 * as well as associating plugins with projects.
 */
export class PluginsApi {
    /**
     * Creates a new PluginsApi instance
     *
     * @param core The core backend API providing HTTP utilities
     */
    constructor(private readonly core: BackendApiCore) {}

    /**
     * Gets all available plugins
     *
     * @returns A promise resolving to an array of all plugins
     */
    async getAll(): Promise<ApiResult<Plugin[], PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/plugins`, {});
    }

    /**
     * Creates a new plugin from a URL
     *
     * @param url The URL where the plugin manifest is hosted
     * @returns A promise resolving to the created plugin's ID
     */
    async create(url: string): Promise<ApiResult<string, PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/plugins`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ url })
        });
    }

    /**
     * Deletes a plugin
     *
     * @param pluginId The ID of the plugin to delete
     * @returns A promise resolving to success or an error
     */
    async delete(pluginId: string): Promise<ApiResult<void, PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/plugins/${pluginId}`, {
            method: "DELETE"
        });
    }

    /**
     * Refreshes a plugin by re-fetching its manifest
     *
     * @param pluginId The ID of the plugin to refresh
     * @returns A promise resolving to success or an error
     */
    async refresh(pluginId: string): Promise<ApiResult<void, PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/plugins/${pluginId}/refresh`, {
            method: "POST"
        });
    }

    /**
     * Refreshes all plugins by re-fetching all plugin manifests
     *
     * @returns A promise resolving to success or an error
     */
    async refreshAll(): Promise<ApiResult<void, PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/plugins/refresh`, {
            method: "POST"
        });
    }

    /**
     * Updates the default status of a plugin
     *
     * @param pluginId The ID of the plugin to update
     * @param isDefault Whether the plugin should be added by default to new projects
     * @returns A promise resolving to success or an error
     */
    async updateDefault(pluginId: string, isDefault: boolean): Promise<ApiResult<void, PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/plugins/${pluginId}/default`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ default: isDefault })
        });
    }

    /**
     * Gets all plugins associated with a project
     *
     * @param projectId The ID of the project
     * @returns A promise resolving to an array of plugins
     */
    async getForProject(projectId: string): Promise<ApiResult<Plugin[], PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/plugins`, {});
    }

    /**
     * Adds a plugin to a project
     *
     * @param projectId The ID of the project
     * @param pluginId The ID of the plugin to add
     * @returns A promise resolving to the added plugin
     */
    async addToProject(projectId: string, pluginId: string): Promise<ApiResult<Plugin, PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/plugins`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ pluginId })
        });
    }

    /**
     * Removes a plugin from a project
     *
     * @param projectId The ID of the project
     * @param pluginId The ID of the plugin to remove
     * @returns A promise resolving to success or an error
     */
    async removeFromProject(projectId: string, pluginId: string): Promise<ApiResult<void, PluginError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/plugins/${pluginId}`, {
            method: "DELETE"
        });
    }
}
