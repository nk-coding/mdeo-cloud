import type {
    IFileDeleteOptions,
    IFileOverwriteOptions,
    IFileWriteOptions,
    FileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { BackendApi, User, UserInfo } from "./backendApi";
import type { Project } from "../project/project";
import type { ApiResult, FileSystemError, ProjectError, PluginError, CommonError } from "./apiResult";
import type { BackendPlugin, ResolvedPlugin } from "./pluginTypes";
import {
    success,
    fileSystemFailure,
    commonFailure,
    CommonErrorCode
} from "./apiResult";

/**
 * Information cached about a file or directory
 */
interface CachedFileInfo {
    type: FileType;
    content?: Uint8Array;
    metadata?: object;
    dirEntries?: [string, FileType][];
}

/**
 * HTTP-based implementation of BackendAPI communicating with the Ktor backend.
 */
export class HttpBackendApi implements BackendApi {
    private baseUrl: string;
    private cachedProjectId: string | null = null;
    private fileTreeCache: Map<string, CachedFileInfo> = new Map();
    
    constructor(baseUrl: string = "/api") {
        this.baseUrl = baseUrl;
    }
    
    
    async getCurrentUser(): Promise<ApiResult<User, CommonError>> {
        try {
            const response = await fetch(`${this.baseUrl}/auth/me`, {
                method: "GET",
                credentials: "include"
            });
            
            if (!response.ok) {
                if (response.status === 401) {
                    return commonFailure(CommonErrorCode.Unknown, "Not authenticated");
                }
                return commonFailure(CommonErrorCode.Unavailable, "Failed to get current user");
            }
            
            const data = await response.json();
            return success(data.value);
        } catch (error) {
            return commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }
    
    async login(username: string, password: string): Promise<ApiResult<User, CommonError>> {
        try {
            const response = await fetch(`${this.baseUrl}/auth/login`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify({ username, password })
            });
            
            if (!response.ok) {
                if (response.status === 401) {
                    return commonFailure(CommonErrorCode.Unknown, "Invalid credentials");
                }
                return commonFailure(CommonErrorCode.Unavailable, "Login failed");
            }
            
            const data = await response.json();
            return success(data.user);
        } catch (error) {
            return commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    async register(username: string, password: string): Promise<ApiResult<User, CommonError>> {
        try {
            const response = await fetch(`${this.baseUrl}/auth/register`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                if (response.status === 409) {
                    return commonFailure(CommonErrorCode.Unknown, "Username already exists");
                }
                return commonFailure(CommonErrorCode.Unavailable, "Registration failed");
            }

            const data = await response.json();
            return success(data.user);
        } catch (error) {
            return commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }
    
    async logout(): Promise<void> {
        await fetch(`${this.baseUrl}/auth/logout`, {
            method: "POST",
            credentials: "include"
        });
    }
    
    async changePassword(currentPassword: string, newPassword: string): Promise<ApiResult<void, CommonError>> {
        try {
            const response = await fetch(`${this.baseUrl}/auth/password`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify({ currentPassword, newPassword })
            });
            
            if (!response.ok) {
                if (response.status === 400) {
                    return commonFailure(CommonErrorCode.Unknown, "Current password is incorrect");
                }
                return commonFailure(CommonErrorCode.Unavailable, "Failed to change password");
            }
            
            return success(undefined);
        } catch (error) {
            return commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }
    
    
    async getProjects(): Promise<ApiResult<Project[], ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects`);
    }
    
    async createProject(name: string): Promise<ApiResult<string, ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects`, {
            method: "POST",
            headers: {

                "Content-Type": "application/json"
            },
            body: JSON.stringify({ name })
        });
    }
    
    async updateProject(projectId: string, updates: { name?: string }): Promise<ApiResult<void, ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}`, {
            method: "PUT",
            headers: {

                "Content-Type": "application/json"
            },
            body: JSON.stringify(updates)
        });
    }
    
    async deleteProject(projectId: string): Promise<ApiResult<void, ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}`, {
            method: "DELETE",
        });
    }
    
    
    async getProjectOwners(projectId: string): Promise<ApiResult<UserInfo[], ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/owners`);
    }
    
    async addProjectOwner(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/owners`, {
            method: "POST",
            headers: {

                "Content-Type": "application/json"
            },
            body: JSON.stringify({ userId })
        });
    }
    
    async removeProjectOwner(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/owners/${userId}`, {
            method: "DELETE",
        });
    }
    
    
    async readFile(projectId: string, path: string): Promise<ApiResult<Uint8Array, FileSystemError>> {
        if (this.cachedProjectId === projectId) {
            const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
            const cached = this.fileTreeCache.get(normalizedPath);
            if (cached?.content !== undefined) {
                return success(cached.content);
            }
        }
        
        try {
            const response = await fetch(`${this.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}`, {
                credentials: "include"
            });
            
            if (!response.ok) {
                return this.parseErrorResponse(response);
            }
            
            const buffer = await response.arrayBuffer();
            return success(new Uint8Array(buffer));
        } catch (error) {
            return fileSystemFailure(CommonErrorCode.Unavailable, String(error));
        }
    }
    
    async writeFile(
        projectId: string,
        path: string,
        content: Uint8Array,
        opts: IFileWriteOptions
    ): Promise<ApiResult<void, FileSystemError>> {
        const params = new URLSearchParams();
        if (opts.create !== undefined) params.set("create", String(opts.create));
        if (opts.overwrite !== undefined) params.set("overwrite", String(opts.overwrite));
        
        return this.fetchApiResult(
            `${this.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}?${params}`,
            {
                method: "POST",
                headers: {

                    "Content-Type": "application/octet-stream"
                },
                body: content as unknown as BodyInit
            }
        );
    }
    
    async mkdir(projectId: string, path: string): Promise<ApiResult<void, FileSystemError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/dirs/${this.encodePath(path)}`, {
            method: "POST",
        });
    }
    
    async readdir(projectId: string, path: string): Promise<ApiResult<[string, FileType][], FileSystemError>> {
        if (this.cachedProjectId === projectId) {
            const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
            const cached = this.fileTreeCache.get(normalizedPath);
            if (cached?.dirEntries !== undefined) {
                return success(cached.dirEntries);
            }
        }

        try {
            const result = await this.fetchApiResult<Array<{ name: string; type: FileType }>>(
                `${this.baseUrl}/projects/${projectId}/dirs/${this.encodePath(path)}`,
                {}
            );
            
            if (!result.success) {
                return result;
            }
            
            const entries: [string, FileType][] = result.value.map(e => [e.name, e.type]);
            return success(entries);
        } catch (error) {
            return fileSystemFailure(CommonErrorCode.Unavailable, String(error));
        }
    }
    
    async stat(projectId: string, path: string): Promise<ApiResult<FileType, FileSystemError>> {
        if (this.cachedProjectId === projectId) {
            const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
            const cached = this.fileTreeCache.get(normalizedPath);
            if (cached !== undefined) {
                return success(cached.type);
            }
        }
        
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/stat/${this.encodePath(path)}`, {
        });
    }
    
    async delete(projectId: string, path: string, opts: IFileDeleteOptions): Promise<ApiResult<void, FileSystemError>> {
        const params = new URLSearchParams();
        if (opts.recursive !== undefined) params.set("recursive", String(opts.recursive));
        
        return this.fetchApiResult(
            `${this.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}?${params}`,
            {
                method: "DELETE",
            }
        );
    }
    
    async rename(
        projectId: string,
        from: string,
        to: string,
        opts: IFileOverwriteOptions
    ): Promise<ApiResult<void, FileSystemError>> {
        const params = new URLSearchParams();
        params.set("from", from);
        params.set("to", to);
        if (opts.overwrite !== undefined) params.set("overwrite", String(opts.overwrite));
        
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/rename?${params}`, {
            method: "POST",
        });
    }
    
    
    async readMetadata(projectId: string, path: string): Promise<ApiResult<object, FileSystemError>> {
        if (this.cachedProjectId === projectId) {
            const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
            const cached = this.fileTreeCache.get(normalizedPath);
            if (cached?.metadata !== undefined) {
                return success(cached.metadata);
            }
        }
        
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/metadata/${this.encodePath(path)}`, {
        });
    }
    
    async writeMetadata(projectId: string, path: string, metadata: object): Promise<ApiResult<void, FileSystemError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/metadata/${this.encodePath(path)}`, {
            method: "PUT",
            headers: {

                "Content-Type": "application/json"
            },
            body: JSON.stringify(metadata)
        });
    }
    
    
    async createPlugin(url: string): Promise<ApiResult<string, PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/plugins`, {
            method: "POST",
            headers: {

                "Content-Type": "application/json"
            },
            body: JSON.stringify({ url })
        });
    }
    
    async deletePlugin(pluginId: string): Promise<ApiResult<void, PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/plugins/${pluginId}`, {
            method: "DELETE",
        });
    }
    
    async getPlugins(): Promise<ApiResult<BackendPlugin[], PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/plugins`, {
        });
    }
    
    async getProjectPlugins(projectId: string): Promise<ApiResult<BackendPlugin[], PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/plugins`, {
        });
    }
    
    async addPluginToProject(projectId: string, pluginId: string): Promise<ApiResult<void, PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/plugins`, {
            method: "POST",
            headers: {

                "Content-Type": "application/json"
            },
            body: JSON.stringify({ pluginId })
        });
    }
    
    async removePluginFromProject(projectId: string, pluginId: string): Promise<ApiResult<void, PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/plugins/${pluginId}`, {
            method: "DELETE",
        });
    }
    
    async resolvePlugin(pluginId: string): Promise<ApiResult<ResolvedPlugin, PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/plugins/${pluginId}/resolve`, {
        });
    }
    
    async precache(project: Project): Promise<void> {
        if (this.cachedProjectId !== project.id) {
            this.fileTreeCache.clear();
            this.cachedProjectId = project.id;
        }
        
        await this.cacheDirectory(project.id, "");
    }
    
    /**
     * Recursively caches a directory and all its contents.
     * @param projectId The ID of the project
     * @param path The path to the directory to cache
     */
    private async cacheDirectory(projectId: string, path: string): Promise<void> {
        const result = await this.fetchApiResult<Array<{ name: string; type: FileType }>>(
            `${this.baseUrl}/projects/${projectId}/dirs/${this.encodePath(path)}`,
            { credentials: "include" }
        );
        
        if (!result.success) {
            return;
        }
        
        const entries: [string, FileType][] = result.value.map(e => [e.name, e.type]);
        
        this.fileTreeCache.set(path, {
            type: 1,
            dirEntries: entries
        });
        
        // Cache all children
        for (const [name, type] of entries) {
            const fullPath = path ? `${path}/${name}` : name;
            
            if (type === 1) {
                await this.cacheDirectory(projectId, fullPath);
            } else {
                await this.cacheFile(projectId, fullPath);
            }
        }
    }
    
    /**
     * Caches a file's content and metadata.
     * @param projectId The ID of the project
     * @param path The path to the file to cache
     */
    private async cacheFile(projectId: string, path: string): Promise<void> {
        // Load file content
        const contentPromise = fetch(`${this.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}`, {
            credentials: "include"
        });
        
        // Load metadata
        const metadataPromise = fetch(`${this.baseUrl}/projects/${projectId}/metadata/${this.encodePath(path)}`, {
            credentials: "include"
        });
        
        const [contentResponse, metadataResponse] = await Promise.all([contentPromise, metadataPromise]);
        
        let content: Uint8Array | undefined;
        let metadata: object | undefined;
        
        if (contentResponse.ok) {
            const buffer = await contentResponse.arrayBuffer();
            content = new Uint8Array(buffer);
        }
        
        if (metadataResponse.ok) {
            try {
                const data = await metadataResponse.json();
                if (typeof data === "object" && "value" in data) {
                    metadata = data.value;
                } else {
                    metadata = data;
                }
            } catch {
            }
        }
        
        this.fileTreeCache.set(path, {
            type: 0,
            content,
            metadata
        });
    }
    
    /**
     * Makes an API request and parses the result.
     * @param url The URL to fetch
     * @param init Optional fetch init options
     * @returns The parsed API result
     */
    private async fetchApiResult<T>(url: string, init?: RequestInit): Promise<ApiResult<T, any>> {
        try {
            const response = await fetch(url, {
                ...init,
                credentials: "include"
            });
            
            if (!response.ok) {
                if (response.status === 401) {
                    return { success: false, error: { code: CommonErrorCode.Unavailable, message: "Not authenticated" } };
                }
                if (response.status === 403) {
                    return { success: false, error: { code: CommonErrorCode.Unavailable, message: "Access denied" } };
                }
                return this.parseErrorResponse(response);
            }
            
            const contentType = response.headers.get("Content-Type");
            if (!contentType || response.status === 204) {
                return success(undefined as T);
            }
            
            const data = await response.json();
            if (typeof data === "object") {
                if ("value" in data) {
                    return success(data.value as T)
                } else if ("error" in data) {
                    return {
                        success: false,
                        error: data.error
                    }
                }
            }
            
            return success(data as T);
        } catch (error) {
            return { success: false, error: { code: CommonErrorCode.Unavailable, message: String(error) } };
        }
    }
    
    /**
     * Parses an error response from the backend.
     * @param response The HTTP response to parse
     * @returns An ApiResult containing the error information
     */
    private async parseErrorResponse<E>(response: Response): Promise<ApiResult<never, E>> {
        try {
            const data = await response.json();
            if (typeof data === "object" && "success" in data && !data.success) {
                return data as ApiResult<never, E>;
            }
            if (typeof data === "object" && "error" in data) {
                return { success: false, error: data.error };
            }
            return { success: false, error: { code: CommonErrorCode.Unknown, message: JSON.stringify(data) } as E };
        } catch {
            return { success: false, error: { code: CommonErrorCode.Unknown, message: response.statusText } as E };
        }
    }
    
    /**
     * Encodes a file path for use in URLs.
     * @param path The path to encode
     * @returns The encoded path
     */
    private encodePath(path: string): string {
        const normalized = path.startsWith("/") ? path.slice(1) : path;
        return normalized.split("/").map(encodeURIComponent).join("/");
    }
}
