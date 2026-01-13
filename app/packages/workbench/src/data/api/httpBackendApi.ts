import {
    type IFileDeleteOptions,
    type IFileOverwriteOptions,
    type IFileWriteOptions,
    FileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { BackendApi, User, UserInfo, FileReadResult } from "./backendApi";
import type { Project } from "../project/project";
import {
    ApiResult,
    type FileSystemError,
    type ProjectError,
    type PluginError,
    type CommonError,
    type FileDataError,
    FileSystemErrorCode
} from "./apiResult";
import type { BackendPlugin, ResolvedPlugin } from "./pluginTypes";
import { CommonErrorCode } from "./apiResult";

/**
 * Information cached about a file or directory
 */
interface CachedFileInfo {
    exists: boolean;
    type?: FileType;
    content?: Uint8Array;
    version?: number;
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
                    return ApiResult.commonFailure(CommonErrorCode.Unknown, "Not authenticated");
                }
                return ApiResult.commonFailure(CommonErrorCode.Unavailable, "Failed to get current user");
            }

            const data = await response.json();
            return ApiResult.success(data.value);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
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
                    return ApiResult.commonFailure(CommonErrorCode.Unknown, "Invalid credentials");
                }
                return ApiResult.commonFailure(CommonErrorCode.Unavailable, "Login failed");
            }

            const data = await response.json();
            return ApiResult.success(data.user);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
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
                    return ApiResult.commonFailure(CommonErrorCode.Unknown, "Username already exists");
                }
                return ApiResult.commonFailure(CommonErrorCode.Unavailable, "Registration failed");
            }

            const data = await response.json();
            return ApiResult.success(data.user);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
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
                    return ApiResult.commonFailure(CommonErrorCode.Unknown, "Current password is incorrect");
                }
                return ApiResult.commonFailure(CommonErrorCode.Unavailable, "Failed to change password");
            }

            return ApiResult.success(undefined);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    async getProjects(): Promise<ApiResult<Project[], ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects`);
    }

    async createProject(name: string): Promise<ApiResult<Project, ProjectError>> {
        return this.fetchApiResult<Project>(`${this.baseUrl}/projects`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ name })
        });
    }

    async updateProject(projectId: string, updates: { name?: string }): Promise<ApiResult<Project, ProjectError>> {
        return this.fetchApiResult<Project>(`${this.baseUrl}/projects/${projectId}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(updates)
        });
    }

    async deleteProject(projectId: string): Promise<ApiResult<void, ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}`, {
            method: "DELETE"
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
            method: "DELETE"
        });
    }

    async readFile(projectId: string, path: string): Promise<ApiResult<FileReadResult, FileSystemError>> {
        const normalizedPath = path.startsWith("/") ? path.slice(1) : path;

        if (this.cachedProjectId === projectId) {
            const cached = this.fileTreeCache.get(normalizedPath);
            if (cached !== undefined) {
                if (!cached.exists) {
                    return ApiResult.fileSystemFailure(FileSystemErrorCode.FileNotFound, "File not found");
                }
                if (cached.type !== FileType.File) {
                    return ApiResult.fileSystemFailure(
                        FileSystemErrorCode.FileIsADirectory,
                        "Cannot read file: path is a directory"
                    );
                }
                if (cached.content !== undefined && cached.version !== undefined) {
                    return ApiResult.success({ content: cached.content, version: cached.version });
                }
            }
        }

        try {
            const response = await fetch(`${this.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}`, {
                credentials: "include"
            });

            if (!response.ok) {
                const errorResult = await this.parseErrorResponse<FileSystemError>(response);

                if (
                    this.cachedProjectId === projectId &&
                    !errorResult.success &&
                    errorResult.error.code === FileSystemErrorCode.FileNotFound
                ) {
                    this.fileTreeCache.set(normalizedPath, {
                        exists: false
                    });
                }

                return errorResult;
            }

            const json = (await response.json()) as { content: string; version: number };
            const content = new TextEncoder().encode(json.content);
            const version = json.version;

            if (this.cachedProjectId === projectId) {
                const existing = this.fileTreeCache.get(normalizedPath);
                this.fileTreeCache.set(normalizedPath, {
                    exists: true,
                    type: FileType.File,
                    content,
                    version,
                    metadata: existing?.metadata
                });
            }

            return ApiResult.success({ content, version });
        } catch (error) {
            return ApiResult.fileSystemFailure(CommonErrorCode.Unavailable, String(error));
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

        const result = await this.fetchApiResult<void>(
            `${this.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}?${params}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/octet-stream"
                },
                body: content as unknown as BodyInit
            }
        );

        if (result.success && this.cachedProjectId === projectId) {
            const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
            const existing = this.fileTreeCache.get(normalizedPath);
            this.fileTreeCache.set(normalizedPath, {
                exists: true,
                type: FileType.File,
                content: content,
                metadata: existing?.metadata
            });

            this.invalidateParentDirCache(normalizedPath);
        }

        return result;
    }

    async mkdir(projectId: string, path: string): Promise<ApiResult<void, FileSystemError>> {
        const result = await this.fetchApiResult<void>(
            `${this.baseUrl}/projects/${projectId}/dirs/${this.encodePath(path)}`,
            {
                method: "POST"
            }
        );

        if (result.success && this.cachedProjectId === projectId) {
            const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
            this.fileTreeCache.set(normalizedPath, {
                exists: true,
                type: FileType.Directory,
                dirEntries: []
            });

            this.invalidateParentDirCache(normalizedPath);
        }

        return result;
    }

    async readdir(projectId: string, path: string): Promise<ApiResult<[string, FileType][], FileSystemError>> {
        const normalizedPath = path.startsWith("/") ? path.slice(1) : path;

        if (this.cachedProjectId === projectId) {
            const cached = this.fileTreeCache.get(normalizedPath);
            if (cached !== undefined) {
                if (!cached.exists) {
                    return ApiResult.fileSystemFailure(FileSystemErrorCode.FileNotFound, "Directory not found");
                }
                if (cached.type !== FileType.Directory) {
                    return ApiResult.fileSystemFailure(
                        FileSystemErrorCode.FileNotADirectory,
                        "Cannot read directory: path is a file"
                    );
                }
                if (cached.dirEntries !== undefined) {
                    return ApiResult.success(cached.dirEntries);
                }
            }
        }

        try {
            const result = await this.fetchApiResult<Array<{ name: string; type: FileType }>>(
                `${this.baseUrl}/projects/${projectId}/dirs/${this.encodePath(path)}`,
                {}
            );

            if (!result.success) {
                if (this.cachedProjectId === projectId && result.error.code === FileSystemErrorCode.FileNotFound) {
                    this.fileTreeCache.set(normalizedPath, {
                        exists: false
                    });
                }
                return result;
            }

            const entries: [string, FileType][] = result.value.map((e) => [e.name, e.type]);

            if (this.cachedProjectId === projectId) {
                const existing = this.fileTreeCache.get(normalizedPath);
                this.fileTreeCache.set(normalizedPath, {
                    exists: true,
                    type: FileType.Directory,
                    dirEntries: entries,
                    metadata: existing?.metadata
                });
            }

            return ApiResult.success(entries);
        } catch (error) {
            return ApiResult.fileSystemFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    async stat(projectId: string, path: string): Promise<ApiResult<FileType, FileSystemError>> {
        const normalizedPath = path.startsWith("/") ? path.slice(1) : path;

        if (this.cachedProjectId === projectId) {
            const cached = this.fileTreeCache.get(normalizedPath);
            if (cached !== undefined) {
                if (!cached.exists) {
                    return ApiResult.fileSystemFailure(FileSystemErrorCode.FileNotFound, "File or directory not found");
                }
                if (cached.type !== undefined) {
                    return ApiResult.success(cached.type);
                }
            }
        }

        const result = await this.fetchApiResult<FileType>(
            `${this.baseUrl}/projects/${projectId}/stat/${this.encodePath(path)}`,
            {}
        );

        if (this.cachedProjectId === projectId) {
            if (result.success) {
                const existing = this.fileTreeCache.get(normalizedPath);
                this.fileTreeCache.set(normalizedPath, {
                    exists: true,
                    type: result.value,
                    content: existing?.content,
                    metadata: existing?.metadata,
                    dirEntries: existing?.dirEntries
                });
            } else if (result.error.code === FileSystemErrorCode.FileNotFound) {
                this.fileTreeCache.set(normalizedPath, {
                    exists: false
                });
            }
        }

        return result;
    }

    async delete(projectId: string, path: string, opts: IFileDeleteOptions): Promise<ApiResult<void, FileSystemError>> {
        const params = new URLSearchParams();
        if (opts.recursive !== undefined) params.set("recursive", String(opts.recursive));

        const result = await this.fetchApiResult<void>(
            `${this.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}?${params}`,
            {
                method: "DELETE"
            }
        );

        if (result.success && this.cachedProjectId === projectId) {
            const normalizedPath = path.startsWith("/") ? path.slice(1) : path;

            this.fileTreeCache.set(normalizedPath, {
                exists: false
            });

            if (opts.recursive) {
                const pathPrefix = normalizedPath ? normalizedPath + "/" : "";
                for (const [cachedPath] of this.fileTreeCache) {
                    if (cachedPath.startsWith(pathPrefix)) {
                        this.fileTreeCache.set(cachedPath, {
                            exists: false
                        });
                    }
                }
            }

            this.invalidateParentDirCache(normalizedPath);
        }

        return result;
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

        const result = await this.fetchApiResult<void>(`${this.baseUrl}/projects/${projectId}/rename?${params}`, {
            method: "POST"
        });

        if (result.success && this.cachedProjectId === projectId) {
            const normalizedFrom = from.startsWith("/") ? from.slice(1) : from;
            const normalizedTo = to.startsWith("/") ? to.slice(1) : to;

            const fromEntry = this.fileTreeCache.get(normalizedFrom);

            if (fromEntry && fromEntry.exists) {
                this.fileTreeCache.set(normalizedTo, fromEntry);

                if (fromEntry.type === FileType.Directory) {
                    const fromPrefix = normalizedFrom ? normalizedFrom + "/" : "";
                    const toPrefix = normalizedTo ? normalizedTo + "/" : "";

                    for (const [cachedPath, entry] of this.fileTreeCache) {
                        if (cachedPath.startsWith(fromPrefix)) {
                            const relativePath = cachedPath.slice(fromPrefix.length);
                            const newPath = toPrefix + relativePath;
                            this.fileTreeCache.set(newPath, entry);
                            this.fileTreeCache.set(cachedPath, { exists: false });
                        }
                    }
                }
            }

            this.fileTreeCache.set(normalizedFrom, {
                exists: false
            });

            this.invalidateParentDirCache(normalizedFrom);
            this.invalidateParentDirCache(normalizedTo);
        }

        return result;
    }

    async readMetadata(projectId: string, path: string): Promise<ApiResult<object, FileSystemError>> {
        const normalizedPath = path.startsWith("/") ? path.slice(1) : path;

        if (this.cachedProjectId === projectId) {
            const cached = this.fileTreeCache.get(normalizedPath);
            if (cached !== undefined) {
                if (!cached.exists) {
                    return ApiResult.fileSystemFailure(FileSystemErrorCode.FileNotFound, "File not found");
                }
                if (cached.metadata !== undefined) {
                    return ApiResult.success(cached.metadata);
                }
            }
        }

        const result = await this.fetchApiResult<object>(
            `${this.baseUrl}/projects/${projectId}/metadata/${this.encodePath(path)}`,
            {}
        );

        if (this.cachedProjectId === projectId) {
            if (result.success) {
                const existing = this.fileTreeCache.get(normalizedPath);
                this.fileTreeCache.set(normalizedPath, {
                    exists: true,
                    type: existing?.type ?? FileType.File,
                    content: existing?.content,
                    metadata: result.value,
                    dirEntries: existing?.dirEntries
                });
            } else if (result.error.code === FileSystemErrorCode.FileNotFound) {
                this.fileTreeCache.set(normalizedPath, {
                    exists: false
                });
            }
        }

        return result;
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
            method: "DELETE"
        });
    }

    async getPlugins(): Promise<ApiResult<BackendPlugin[], PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/plugins`, {});
    }

    async getProjectPlugins(projectId: string): Promise<ApiResult<BackendPlugin[], PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/plugins`, {});
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
            method: "DELETE"
        });
    }

    async resolvePlugin(pluginId: string): Promise<ApiResult<ResolvedPlugin, PluginError>> {
        return this.fetchApiResult(`${this.baseUrl}/plugins/${pluginId}/resolve`, {});
    }

    async precache(project: Project): Promise<void> {
        if (this.cachedProjectId !== project.id) {
            this.fileTreeCache.clear();
            this.cachedProjectId = project.id;
        }

        await this.walkDirectory(project.id, "");
    }

    async getAllUsers(): Promise<ApiResult<User[], CommonError>> {
        return this.fetchApiResult(`${this.baseUrl}/users`, {
            method: "GET"
        });
    }

    async getUserProjects(userId: string): Promise<ApiResult<Project[], ProjectError>> {
        return this.fetchApiResult(`${this.baseUrl}/users/${userId}/projects`, {
            method: "GET"
        });
    }

    async updateUserAdmin(userId: string, isAdmin: boolean): Promise<ApiResult<void, CommonError>> {
        return this.fetchApiResult(`${this.baseUrl}/users/${userId}/admin`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ isAdmin })
        });
    }

    /**
     * Invalidates the directory entries cache for the parent directory of a given path.
     * This should be called when a file or directory is created, deleted, or renamed.
     * @param path The path whose parent directory cache should be invalidated
     */
    private invalidateParentDirCache(path: string): void {
        const lastSlashIndex = path.lastIndexOf("/");
        if (lastSlashIndex === -1) {
            const rootCache = this.fileTreeCache.get("");
            if (rootCache && rootCache.exists && rootCache.type === FileType.Directory) {
                this.fileTreeCache.set("", {
                    exists: true,
                    type: FileType.Directory,
                    dirEntries: undefined,
                    metadata: rootCache.metadata
                });
            }
        } else {
            const parentPath = path.slice(0, lastSlashIndex);
            const parentCache = this.fileTreeCache.get(parentPath);
            if (parentCache && parentCache.exists && parentCache.type === FileType.Directory) {
                this.fileTreeCache.set(parentPath, {
                    exists: true,
                    type: FileType.Directory,
                    dirEntries: undefined,
                    metadata: parentCache.metadata
                });
            }
        }
    }

    /**
     * Recursively walks a directory and caches all files and subdirectories.
     * Uses regular API methods which handle caching automatically.
     * @param projectId The ID of the project
     * @param path The path to the directory to walk
     */
    private async walkDirectory(projectId: string, path: string): Promise<void> {
        const dirResult = await this.readdir(projectId, path);

        if (!dirResult.success) {
            return;
        }

        for (const [name, type] of dirResult.value) {
            const fullPath = path ? `${path}/${name}` : name;

            if (type === FileType.Directory) {
                await this.walkDirectory(projectId, fullPath);
            } else {
                await Promise.all([this.readFile(projectId, fullPath), this.readMetadata(projectId, fullPath)]);
            }
        }
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
                    return {
                        success: false,
                        error: { code: CommonErrorCode.Unavailable, message: "Not authenticated" }
                    };
                }
                if (response.status === 403) {
                    return { success: false, error: { code: CommonErrorCode.Unavailable, message: "Access denied" } };
                }
                return this.parseErrorResponse(response);
            }

            const contentType = response.headers.get("Content-Type");
            if (!contentType || response.status === 204) {
                return ApiResult.success(undefined as T);
            }

            const data = await response.json();
            if (typeof data === "object") {
                if ("value" in data) {
                    return ApiResult.success(data.value as T);
                } else if ("error" in data) {
                    return {
                        success: false,
                        error: data.error
                    };
                }
            }

            return ApiResult.success(data as T);
        } catch (error) {
            return ApiResult.fileSystemFailure(CommonErrorCode.Unavailable, String(error));
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

    async getFileData(projectId: string, path: string, key: string): Promise<ApiResult<string, FileDataError>> {
        const params = new URLSearchParams({ path });
        return this.fetchApiResult(`${this.baseUrl}/projects/${projectId}/file-data/${encodeURIComponent(key)}?${params}`, {
            method: "GET"
        });
    }
}
