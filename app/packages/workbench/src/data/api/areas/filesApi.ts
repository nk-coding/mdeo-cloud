import {
    type IFileDeleteOptions,
    type IFileOverwriteOptions,
    type IFileWriteOptions,
    FileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import { ApiResult, type FileSystemError, FileSystemErrorCode, CommonErrorCode } from "../apiResult";
import type { BackendApiCore } from "../backendApi";
import type { Project } from "../../project/project";

/**
 * Response from reading a file, including content and version
 */
export interface FileReadResult {
    content: Uint8Array;
    version: number;
}

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
 * API for file system operations within projects.
 * Provides methods for reading, writing, and managing files and directories
 * with an internal caching layer for performance optimization.
 */
export class FilesApi {
    private cachedProjectId: string | undefined = undefined;
    private fileTreeCache: Map<string, CachedFileInfo> = new Map();

    /**
     * Creates a new FilesApi instance
     *
     * @param core The core backend API providing HTTP utilities
     */
    constructor(private readonly core: BackendApiCore) {}

    /**
     * Reads the contents of a file along with its version
     *
     * @param projectId The ID of the project containing the file
     * @param path The path to the file relative to the project root
     * @returns A promise resolving to the file contents and version
     */
    async readFile(projectId: string, path: string): Promise<ApiResult<FileReadResult, FileSystemError>> {
        const normalizedPath = this.normalizePath(path);
        const cachedResult = this.checkFileCache(projectId, normalizedPath);
        if (cachedResult !== null) {
            return cachedResult;
        }

        try {
            const response = await fetch(`${this.core.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}`, {
                credentials: "include"
            });

            if (!response.ok) {
                return this.handleReadFileError(projectId, normalizedPath, response);
            }

            return this.processReadFileResponse(projectId, normalizedPath, response);
        } catch (error) {
            return ApiResult.fileSystemFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    /**
     * Writes content to a file
     *
     * @param projectId The ID of the project containing the file
     * @param path The path to the file relative to the project root
     * @param content The content to write as a Uint8Array
     * @param opts Write options (create, overwrite)
     * @returns A promise resolving to success or an error
     */
    async writeFile(
        projectId: string,
        path: string,
        content: Uint8Array,
        opts: IFileWriteOptions
    ): Promise<ApiResult<void, FileSystemError>> {
        const params = new URLSearchParams();
        if (opts.create !== undefined) params.set("create", String(opts.create));
        if (opts.overwrite !== undefined) params.set("overwrite", String(opts.overwrite));

        const result = await this.core.fetchApiResult<void>(
            `${this.core.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}?${params}`,
            {
                method: "POST",
                headers: { "Content-Type": "application/octet-stream" },
                body: content as unknown as BodyInit
            }
        );

        if (result.success && this.cachedProjectId === projectId) {
            this.updateWriteCache(path, content);
        }

        return result;
    }

    /**
     * Creates a new directory
     *
     * @param projectId The ID of the project
     * @param path The path for the new directory
     * @returns A promise resolving to success or an error
     */
    async mkdir(projectId: string, path: string): Promise<ApiResult<void, FileSystemError>> {
        const result = await this.core.fetchApiResult<void>(
            `${this.core.baseUrl}/projects/${projectId}/dirs/${this.encodePath(path)}`,
            { method: "POST" }
        );

        if (result.success && this.cachedProjectId === projectId) {
            const normalizedPath = this.normalizePath(path);
            this.fileTreeCache.set(normalizedPath, {
                exists: true,
                type: FileType.Directory,
                dirEntries: []
            });
            this.invalidateParentDirCache(normalizedPath);
        }

        return result;
    }

    /**
     * Reads the contents of a directory
     *
     * @param projectId The ID of the project
     * @param path The path to the directory
     * @returns A promise resolving to an array of [name, FileType] tuples
     */
    async readdir(projectId: string, path: string): Promise<ApiResult<[string, FileType][], FileSystemError>> {
        const normalizedPath = this.normalizePath(path);
        const cachedResult = this.checkDirCache(projectId, normalizedPath);
        if (cachedResult !== null) {
            return cachedResult;
        }

        try {
            const result = await this.core.fetchApiResult<Array<{ name: string; type: FileType }>>(
                `${this.core.baseUrl}/projects/${projectId}/dirs/${this.encodePath(path)}`,
                {}
            );

            if (!result.success) {
                this.handleReaddirCacheMiss(projectId, normalizedPath, result);
                return result;
            }

            return this.processReaddirResponse(projectId, normalizedPath, result.value);
        } catch (error) {
            return ApiResult.fileSystemFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    /**
     * Gets the type of a file or directory
     *
     * @param projectId The ID of the project
     * @param path The path to stat
     * @returns A promise resolving to the FileType or undefined
     */
    async stat(projectId: string, path: string): Promise<ApiResult<FileType | undefined, FileSystemError>> {
        const normalizedPath = this.normalizePath(path);
        const cachedResult = this.checkStatCache(projectId, normalizedPath);
        if (cachedResult !== null) {
            return cachedResult;
        }

        const result = await this.core.fetchApiResult<FileType | undefined>(
            `${this.core.baseUrl}/projects/${projectId}/stat/${this.encodePath(path)}`,
            {}
        );

        if (this.cachedProjectId === projectId && result.success) {
            this.updateStatCache(normalizedPath, result.value);
        }

        return result;
    }

    /**
     * Deletes a file or directory
     *
     * @param projectId The ID of the project
     * @param path The path to delete
     * @param opts Delete options (recursive)
     * @returns A promise resolving to success or an error
     */
    async delete(projectId: string, path: string, opts: IFileDeleteOptions): Promise<ApiResult<void, FileSystemError>> {
        const params = new URLSearchParams();
        if (opts.recursive !== undefined) params.set("recursive", String(opts.recursive));

        const result = await this.core.fetchApiResult<void>(
            `${this.core.baseUrl}/projects/${projectId}/files/${this.encodePath(path)}?${params}`,
            { method: "DELETE" }
        );

        if (result.success && this.cachedProjectId === projectId) {
            this.updateDeleteCache(path, opts);
        }

        return result;
    }

    /**
     * Renames or moves a file or directory
     *
     * @param projectId The ID of the project
     * @param from The current path
     * @param to The new path
     * @param opts Rename options (overwrite)
     * @returns A promise resolving to success or an error
     */
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

        const result = await this.core.fetchApiResult<void>(
            `${this.core.baseUrl}/projects/${projectId}/rename?${params}`,
            { method: "POST" }
        );

        if (result.success && this.cachedProjectId === projectId) {
            this.updateRenameCache(from, to);
        }

        return result;
    }

    /**
     * Reads metadata for a file
     *
     * @param projectId The ID of the project
     * @param path The path to the file
     * @returns A promise resolving to the metadata object
     */
    async readMetadata(projectId: string, path: string): Promise<ApiResult<object, FileSystemError>> {
        const normalizedPath = this.normalizePath(path);
        const cachedResult = this.checkMetadataCache(projectId, normalizedPath);
        if (cachedResult !== null) {
            return cachedResult;
        }

        const result = await this.core.fetchApiResult<object>(
            `${this.core.baseUrl}/projects/${projectId}/metadata/${this.encodePath(path)}`,
            {}
        );

        if (this.cachedProjectId === projectId) {
            this.updateMetadataReadCache(normalizedPath, result);
        }

        return result;
    }

    /**
     * Writes metadata for a file
     *
     * @param projectId The ID of the project
     * @param path The path to the file
     * @param metadata The metadata object to write
     * @returns A promise resolving to success or an error
     */
    async writeMetadata(projectId: string, path: string, metadata: object): Promise<ApiResult<void, FileSystemError>> {
        const result = await this.core.fetchApiResult<void>(
            `${this.core.baseUrl}/projects/${projectId}/metadata/${this.encodePath(path)}`,
            {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(metadata)
            }
        );

        if (result.success && this.cachedProjectId === projectId) {
            const normalizedPath = this.normalizePath(path);
            const existing = this.fileTreeCache.get(normalizedPath);
            if (existing != undefined) {
                this.fileTreeCache.set(normalizedPath, { ...existing, metadata: metadata });
            }
        }
        return result;
    }

    /**
     * Precaches the file tree for a project to optimize subsequent operations.
     * When called with a different project, the previous cache is dropped.
     *
     * @param project The project to precache
     */
    async precache(project: Project): Promise<void> {
        if (this.cachedProjectId !== project.id) {
            this.fileTreeCache.clear();
            this.cachedProjectId = project.id;
        }
        await this.walkDirectory(project.id, "");
    }

    private checkFileCache(
        projectId: string,
        normalizedPath: string
    ): ApiResult<FileReadResult, FileSystemError> | null {
        if (this.cachedProjectId !== projectId) {
            return null;
        }

        const cached = this.fileTreeCache.get(normalizedPath);
        if (cached === undefined) {
            return null;
        }

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
        return null;
    }

    private async handleReadFileError(
        projectId: string,
        normalizedPath: string,
        response: Response
    ): Promise<ApiResult<FileReadResult, FileSystemError>> {
        const errorResult = await this.core.parseErrorResponse<FileSystemError>(response);

        if (
            this.cachedProjectId === projectId &&
            !errorResult.success &&
            errorResult.error.code === FileSystemErrorCode.FileNotFound
        ) {
            this.fileTreeCache.set(normalizedPath, { exists: false });
        }

        return errorResult;
    }

    private async processReadFileResponse(
        projectId: string,
        normalizedPath: string,
        response: Response
    ): Promise<ApiResult<FileReadResult, FileSystemError>> {
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
    }

    private updateWriteCache(path: string, content: Uint8Array): void {
        const normalizedPath = this.normalizePath(path);
        const existing = this.fileTreeCache.get(normalizedPath);
        this.fileTreeCache.set(normalizedPath, {
            exists: true,
            type: FileType.File,
            content: content,
            metadata: existing?.metadata
        });
        this.invalidateParentDirCache(normalizedPath);
    }

    private checkDirCache(
        projectId: string,
        normalizedPath: string
    ): ApiResult<[string, FileType][], FileSystemError> | null {
        if (this.cachedProjectId !== projectId) {
            return null;
        }

        const cached = this.fileTreeCache.get(normalizedPath);
        if (cached === undefined) {
            return null;
        }

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
        return null;
    }

    private handleReaddirCacheMiss(
        projectId: string,
        normalizedPath: string,
        result: ApiResult<Array<{ name: string; type: FileType }>, FileSystemError>
    ): void {
        if (
            this.cachedProjectId === projectId &&
            !result.success &&
            result.error.code === FileSystemErrorCode.FileNotFound
        ) {
            this.fileTreeCache.set(normalizedPath, { exists: false });
        }
    }

    private processReaddirResponse(
        projectId: string,
        normalizedPath: string,
        value: Array<{ name: string; type: FileType }>
    ): ApiResult<[string, FileType][], FileSystemError> {
        const entries: [string, FileType][] = value.map((e) => [e.name, e.type]);

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
    }

    private checkStatCache(
        projectId: string,
        normalizedPath: string
    ): ApiResult<FileType | undefined, FileSystemError> | null {
        if (this.cachedProjectId !== projectId) {
            return null;
        }

        const cached = this.fileTreeCache.get(normalizedPath);
        if (cached === undefined) {
            return null;
        }

        if (!cached.exists) {
            return ApiResult.fileSystemFailure(FileSystemErrorCode.FileNotFound, "File or directory not found");
        }
        if (cached.type !== undefined) {
            return ApiResult.success(cached.type);
        }
        return null;
    }

    private updateStatCache(normalizedPath: string, value: FileType | undefined): void {
        if (value != undefined) {
            const existing = this.fileTreeCache.get(normalizedPath);
            this.fileTreeCache.set(normalizedPath, {
                exists: true,
                type: value,
                content: existing?.content,
                metadata: existing?.metadata,
                dirEntries: existing?.dirEntries
            });
        } else {
            this.fileTreeCache.set(normalizedPath, { exists: false });
        }
    }

    private updateDeleteCache(path: string, opts: IFileDeleteOptions): void {
        const normalizedPath = this.normalizePath(path);
        this.fileTreeCache.set(normalizedPath, { exists: false });

        if (opts.recursive) {
            const pathPrefix = normalizedPath ? normalizedPath + "/" : "";
            for (const [cachedPath] of this.fileTreeCache) {
                if (cachedPath.startsWith(pathPrefix)) {
                    this.fileTreeCache.set(cachedPath, { exists: false });
                }
            }
        }

        this.invalidateParentDirCache(normalizedPath);
    }

    private updateRenameCache(from: string, to: string): void {
        const normalizedFrom = from.startsWith("/") ? from.slice(1) : from;
        const normalizedTo = to.startsWith("/") ? to.slice(1) : to;

        const fromEntry = this.fileTreeCache.get(normalizedFrom);

        if (fromEntry && fromEntry.exists) {
            this.fileTreeCache.set(normalizedTo, fromEntry);

            if (fromEntry.type === FileType.Directory) {
                this.moveDirectoryInCache(normalizedFrom, normalizedTo);
            }
        }

        this.fileTreeCache.set(normalizedFrom, { exists: false });

        this.invalidateParentDirCache(normalizedFrom);
        this.invalidateParentDirCache(normalizedTo);
    }

    private moveDirectoryInCache(normalizedFrom: string, normalizedTo: string): void {
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

    private checkMetadataCache(projectId: string, normalizedPath: string): ApiResult<object, FileSystemError> | null {
        if (this.cachedProjectId !== projectId) {
            return null;
        }

        const cached = this.fileTreeCache.get(normalizedPath);
        if (cached === undefined) {
            return null;
        }

        if (!cached.exists) {
            return ApiResult.fileSystemFailure(FileSystemErrorCode.FileNotFound, "File not found");
        }
        if (cached.metadata !== undefined) {
            return ApiResult.success(cached.metadata);
        }
        return null;
    }

    private updateMetadataReadCache(normalizedPath: string, result: ApiResult<object, FileSystemError>): void {
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
            this.fileTreeCache.set(normalizedPath, { exists: false });
        }
    }

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

    private encodePath(path: string): string {
        const normalized = path.startsWith("/") ? path.slice(1) : path;
        return normalized.split("/").map(encodeURIComponent).join("/");
    }

    private normalizePath(path: string): string {
        return path.startsWith("/") ? path.slice(1) : path;
    }
}
