import type {
    IFileDeleteOptions,
    IFileOverwriteOptions,
    IFileWriteOptions,
    FileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import { FileType as VSCodeFileType } from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { BackendApi } from "./backendApi";
import type { Project } from "../project/project";
import type { ApiResult, FileSystemError, ProjectError } from "./apiResult";
import {
    success,
    fileSystemFailure,
    projectFailure,
    FileSystemErrorCode,
    ProjectErrorCode,
    CommonErrorCode
} from "./apiResult";

interface FileEntry {
    type: FileType;
    content?: Uint8Array;
    children?: string[];
}

interface ProjectData {
    id: string;
    name: string;
}

/**
 * Browser-based implementation of BackendAPI using IndexedDB for storage.
 */
export class BrowserBackendApi implements BackendApi {
    private dbName = "mdeo-workbench";
    private dbVersion = 2;
    private db: IDBDatabase | null = null;

    constructor() {
        this.initDB();
    }

    /**
     * Initializes the IndexedDB database with required object stores.
     */
    private async initDB(): Promise<void> {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.dbName, this.dbVersion);

            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                this.db = request.result;
                resolve();
            };

            request.onupgradeneeded = (event) => {
                const db = (event.target as IDBOpenDBRequest).result;

                // Store for projects
                if (!db.objectStoreNames.contains("projects")) {
                    db.createObjectStore("projects", { keyPath: "id" });
                }

                // Store for file system entries
                // Key format: "projectId/path"
                if (!db.objectStoreNames.contains("files")) {
                    const fileStore = db.createObjectStore("files");
                    fileStore.createIndex("projectId", "projectId", { unique: false });
                }

                // Store for file metadata
                // Key format: "projectId/path"
                if (!db.objectStoreNames.contains("metadata")) {
                    db.createObjectStore("metadata");
                }
            };
        });
    }

    /**
     * Ensures the database is initialized.
     */
    private async ensureDB(): Promise<IDBDatabase> {
        if (!this.db) {
            await this.initDB();
        }
        if (!this.db) {
            throw new Error("Failed to initialize database");
        }
        return this.db;
    }

    /**
     * Creates a key for file storage.
     */
    private makeFileKey(projectId: string, path: string): string {
        // Normalize path to ensure consistency
        const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
        return `${projectId}/${normalizedPath}`;
    }

    /**
     * Gets a file entry from the database.
     */
    private async getFileEntry(projectId: string, path: string): Promise<FileEntry | null> {
        const db = await this.ensureDB();
        const key = this.makeFileKey(projectId, path);

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["files"], "readonly");
            const store = transaction.objectStore("files");
            const request = store.get(key);

            request.onsuccess = () => resolve(request.result || null);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Sets a file entry in the database.
     */
    private async setFileEntry(projectId: string, path: string, entry: FileEntry): Promise<void> {
        const db = await this.ensureDB();
        const key = this.makeFileKey(projectId, path);

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["files"], "readwrite");
            const store = transaction.objectStore("files");
            const request = store.put({ ...entry, projectId }, key);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Deletes a file entry from the database.
     */
    private async deleteFileEntry(projectId: string, path: string): Promise<void> {
        const db = await this.ensureDB();
        const key = this.makeFileKey(projectId, path);

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["files"], "readwrite");
            const store = transaction.objectStore("files");
            const request = store.delete(key);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Gets the parent directory path.
     */
    private getParentPath(path: string): string | null {
        const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
        if (!normalizedPath || normalizedPath === "/") return null;

        const lastSlash = normalizedPath.lastIndexOf("/");
        if (lastSlash === -1) return "";
        return normalizedPath.slice(0, lastSlash);
    }

    /**
     * Gets the basename of a path.
     */
    private getBasename(path: string): string {
        const normalizedPath = path.startsWith("/") ? path.slice(1) : path;
        const lastSlash = normalizedPath.lastIndexOf("/");
        return lastSlash === -1 ? normalizedPath : normalizedPath.slice(lastSlash + 1);
    }

    /**
     * Ensures all parent directories exist.
     */
    private async ensureParentDirectories(projectId: string, path: string): Promise<void> {
        const parentPath = this.getParentPath(path);
        if (parentPath === null) return;

        const parent = await this.getFileEntry(projectId, parentPath);
        if (!parent) {
            // Create parent directory recursively
            await this.ensureParentDirectories(projectId, parentPath);
            await this.setFileEntry(projectId, parentPath, {
                type: VSCodeFileType.Directory,
                children: []
            });
        }
    }

    /**
     * Adds a child to a directory's children list.
     */
    private async addChildToDirectory(projectId: string, dirPath: string, childName: string): Promise<void> {
        const dir = await this.getFileEntry(projectId, dirPath);
        if (!dir || dir.type !== VSCodeFileType.Directory) {
            throw new Error(`Not a directory: ${dirPath}`);
        }

        if (!dir.children) {
            dir.children = [];
        }

        if (!dir.children.includes(childName)) {
            dir.children.push(childName);
            await this.setFileEntry(projectId, dirPath, dir);
        }
    }

    /**
     * Removes a child from a directory's children list.
     */
    private async removeChildFromDirectory(projectId: string, dirPath: string, childName: string): Promise<void> {
        const dir = await this.getFileEntry(projectId, dirPath);
        if (!dir || dir.type !== VSCodeFileType.Directory) {
            return;
        }

        if (dir.children) {
            dir.children = dir.children.filter((c) => c !== childName);
            await this.setFileEntry(projectId, dirPath, dir);
        }
    }

    async readFile(projectId: string, path: string): Promise<ApiResult<Uint8Array, FileSystemError>> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            return fileSystemFailure(FileSystemErrorCode.FileNotFound, `File not found: ${path}`);
        }

        if (entry.type === VSCodeFileType.Directory) {
            return fileSystemFailure(FileSystemErrorCode.FileIsADirectory, `Is a directory: ${path}`);
        }

        return success(entry.content || new Uint8Array(0));
    }

    async writeFile(
        projectId: string,
        path: string,
        content: Uint8Array,
        opts: IFileWriteOptions
    ): Promise<ApiResult<void, FileSystemError>> {
        const entry = await this.getFileEntry(projectId, path);

        if (entry) {
            if (entry.type === VSCodeFileType.Directory) {
                return fileSystemFailure(FileSystemErrorCode.FileIsADirectory, `Is a directory: ${path}`);
            }
            if (!opts.overwrite) {
                return fileSystemFailure(FileSystemErrorCode.FileExists, `File already exists: ${path}`);
            }
        } else {
            if (!opts.create) {
                return fileSystemFailure(FileSystemErrorCode.FileNotFound, `File not found: ${path}`);
            }

            // Ensure parent directories exist
            await this.ensureParentDirectories(projectId, path);

            // Add to parent directory
            const parentPath = this.getParentPath(path);
            if (parentPath !== null) {
                const basename = this.getBasename(path);
                await this.addChildToDirectory(projectId, parentPath, basename);
            }
        }

        await this.setFileEntry(projectId, path, {
            type: VSCodeFileType.File,
            content
        });

        return success(undefined);
    }

    async mkdir(projectId: string, path: string): Promise<ApiResult<void, FileSystemError>> {
        const entry = await this.getFileEntry(projectId, path);

        if (entry) {
            if (entry.type === VSCodeFileType.Directory) {
                // Directory already exists, this is OK
                return success(undefined);
            }
            return fileSystemFailure(FileSystemErrorCode.FileExists, `File already exists: ${path}`);
        }

        // Ensure parent directories exist
        await this.ensureParentDirectories(projectId, path);

        // Add to parent directory
        const parentPath = this.getParentPath(path);
        if (parentPath !== null) {
            const basename = this.getBasename(path);
            await this.addChildToDirectory(projectId, parentPath, basename);
        }

        await this.setFileEntry(projectId, path, {
            type: VSCodeFileType.Directory,
            children: []
        });

        return success(undefined);
    }

    async readdir(projectId: string, path: string): Promise<ApiResult<[string, FileType][], FileSystemError>> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            return fileSystemFailure(FileSystemErrorCode.FileNotFound, `Directory not found: ${path}`);
        }

        if (entry.type !== VSCodeFileType.Directory) {
            return fileSystemFailure(FileSystemErrorCode.FileNotADirectory, `Not a directory: ${path}`);
        }

        const children = entry.children || [];
        const result: [string, FileType][] = [];

        for (const child of children) {
            const childPath = path != "/" ? `${path}/${child}` : child;
            const childEntry = await this.getFileEntry(projectId, childPath);
            if (childEntry) {
                result.push([child, childEntry.type]);
            }
        }
        return success(result);
    }

    async stat(projectId: string, path: string): Promise<ApiResult<FileType, FileSystemError>> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            return fileSystemFailure(FileSystemErrorCode.FileNotFound, `File or directory not found: ${path}`);
        }

        return success(entry.type);
    }

    async delete(projectId: string, path: string, opts: IFileDeleteOptions): Promise<ApiResult<void, FileSystemError>> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            return fileSystemFailure(FileSystemErrorCode.FileNotFound, `File or directory not found: ${path}`);
        }

        if (entry.type === VSCodeFileType.Directory) {
            const children = entry.children || [];
            if (children.length > 0 && !opts.recursive) {
                return fileSystemFailure(FileSystemErrorCode.DirectoryNotEmpty, `Directory not empty: ${path}`);
            }

            // Delete all children recursively
            for (const child of children) {
                const childPath = path ? `${path}/${child}` : child;
                const deleteResult = await this.delete(projectId, childPath, { ...opts, recursive: true });
                if (!deleteResult.success) {
                    return deleteResult;
                }
            }
        }

        // Remove from parent directory
        const parentPath = this.getParentPath(path);
        if (parentPath !== null) {
            const basename = this.getBasename(path);
            await this.removeChildFromDirectory(projectId, parentPath, basename);
        }

        await this.deleteFileEntry(projectId, path);

        // Delete metadata if it exists
        await this.deleteMetadata(projectId, path).catch(() => {
            // Ignore errors if metadata doesn't exist
        });

        return success(undefined);
    }

    async rename(
        projectId: string,
        from: string,
        to: string,
        opts: IFileOverwriteOptions
    ): Promise<ApiResult<void, FileSystemError>> {
        const fromEntry = await this.getFileEntry(projectId, from);

        if (!fromEntry) {
            return fileSystemFailure(FileSystemErrorCode.FileNotFound, `File or directory not found: ${from}`);
        }

        const toEntry = await this.getFileEntry(projectId, to);
        if (toEntry && !opts.overwrite) {
            return fileSystemFailure(FileSystemErrorCode.FileExists, `File or directory already exists: ${to}`);
        }

        // If destination exists and overwrite is allowed, delete it first
        if (toEntry) {
            const deleteResult = await this.delete(projectId, to, { recursive: true, useTrash: false, atomic: false });
            if (!deleteResult.success) {
                return deleteResult;
            }
        }

        // Ensure parent directories exist for destination
        await this.ensureParentDirectories(projectId, to);

        // Handle directory rename/move
        if (fromEntry.type === VSCodeFileType.Directory) {
            const children = fromEntry.children || [];

            // Create new directory
            const mkdirResult = await this.mkdir(projectId, to);
            if (!mkdirResult.success) {
                return mkdirResult;
            }

            // Move all children
            for (const child of children) {
                const fromChildPath = from ? `${from}/${child}` : child;
                const toChildPath = to ? `${to}/${child}` : child;
                const renameResult = await this.rename(projectId, fromChildPath, toChildPath, { overwrite: true });
                if (!renameResult.success) {
                    return renameResult;
                }
            }
        } else {
            // Move file
            await this.setFileEntry(projectId, to, fromEntry);

            // Move metadata if it exists
            const metadata = await this.getMetadata(projectId, from);
            if (metadata !== null) {
                await this.setMetadata(projectId, to, metadata);
                await this.deleteMetadata(projectId, from);
            }

            // Add to new parent directory
            const toParentPath = this.getParentPath(to);
            if (toParentPath !== null) {
                const toBasename = this.getBasename(to);
                await this.addChildToDirectory(projectId, toParentPath, toBasename);
            }
        }

        // Remove from old parent directory
        const fromParentPath = this.getParentPath(from);
        if (fromParentPath !== null) {
            const fromBasename = this.getBasename(from);
            await this.removeChildFromDirectory(projectId, fromParentPath, fromBasename);
        }

        // Delete old entry
        await this.deleteFileEntry(projectId, from);
        return success(undefined);
    }

    async getProjects(): Promise<ApiResult<Project[], ProjectError>> {
        const db = await this.ensureDB();

        return new Promise((resolve) => {
            const transaction = db.transaction(["projects"], "readonly");
            const store = transaction.objectStore("projects");
            const request = store.getAll();

            request.onsuccess = () => {
                const projects = request.result as ProjectData[];
                resolve(success(projects.map((p) => ({ id: p.id, name: p.name }))));
            };
            request.onerror = () => {
                resolve(
                    projectFailure(
                        CommonErrorCode.Unknown,
                        `Failed to get projects: ${request.error?.message || "Unknown error"}`
                    )
                );
            };
        });
    }

    async createProject(name: string): Promise<ApiResult<string, ProjectError>> {
        const db = await this.ensureDB();
        const id = `project-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

        return new Promise((resolve) => {
            const transaction = db.transaction(["projects", "files"], "readwrite");
            const projectStore = transaction.objectStore("projects");
            const fileStore = transaction.objectStore("files");

            const projectData: ProjectData = { id, name };
            const projectRequest = projectStore.add(projectData);

            projectRequest.onerror = () => {
                resolve(
                    projectFailure(
                        CommonErrorCode.Unknown,
                        `Failed to create project: ${projectRequest.error?.message || "Unknown error"}`
                    )
                );
            };

            // Create root directory for the project
            const rootKey = this.makeFileKey(id, "");
            const rootEntry: FileEntry = {
                type: VSCodeFileType.Directory,
                children: []
            };
            const fileRequest = fileStore.put({ ...rootEntry, projectId: id }, rootKey);

            fileRequest.onerror = () => {
                resolve(
                    projectFailure(
                        CommonErrorCode.Unknown,
                        `Failed to create project root: ${fileRequest.error?.message || "Unknown error"}`
                    )
                );
            };

            transaction.oncomplete = () => resolve(success(id));
            transaction.onerror = () => {
                resolve(
                    projectFailure(
                        CommonErrorCode.Unknown,
                        `Failed to create project: ${transaction.error?.message || "Unknown error"}`
                    )
                );
            };
        });
    }

    async updateProject(projectId: string, updates: { name?: string }): Promise<ApiResult<void, ProjectError>> {
        const db = await this.ensureDB();

        return new Promise((resolve) => {
            const transaction = db.transaction(["projects"], "readwrite");
            const store = transaction.objectStore("projects");
            const getRequest = store.get(projectId);

            getRequest.onsuccess = () => {
                const project = getRequest.result as ProjectData | undefined;
                if (!project) {
                    resolve(projectFailure(ProjectErrorCode.ProjectNotFound, `Project not found: ${projectId}`));
                    return;
                }

                const updatedProject: ProjectData = {
                    ...project,
                    ...updates
                };

                const putRequest = store.put(updatedProject);
                putRequest.onsuccess = () => resolve(success(undefined));
                putRequest.onerror = () => {
                    resolve(
                        projectFailure(
                            CommonErrorCode.Unknown,
                            `Failed to update project: ${putRequest.error?.message || "Unknown error"}`
                        )
                    );
                };
            };

            getRequest.onerror = () => {
                resolve(
                    projectFailure(
                        CommonErrorCode.Unknown,
                        `Failed to get project: ${getRequest.error?.message || "Unknown error"}`
                    )
                );
            };
        });
    }

    async deleteProject(projectId: string): Promise<ApiResult<void, ProjectError>> {
        const db = await this.ensureDB();

        return new Promise((resolve) => {
            const transaction = db.transaction(["projects", "files", "metadata"], "readwrite");
            const projectStore = transaction.objectStore("projects");
            const fileStore = transaction.objectStore("files");
            const metadataStore = transaction.objectStore("metadata");

            // Delete project
            const deleteProjectRequest = projectStore.delete(projectId);
            deleteProjectRequest.onerror = () => {
                resolve(
                    projectFailure(
                        CommonErrorCode.Unknown,
                        `Failed to delete project: ${deleteProjectRequest.error?.message || "Unknown error"}`
                    )
                );
            };

            // Collect all keys to delete
            const keysToDelete: IDBValidKey[] = [];

            // Delete all files for this project
            const index = fileStore.index("projectId");
            const range = IDBKeyRange.only(projectId);
            const cursorRequest = index.openCursor(range);

            cursorRequest.onsuccess = (event) => {
                const cursor = (event.target as IDBRequest<IDBCursorWithValue>).result;
                if (cursor) {
                    const key = cursor.primaryKey;
                    keysToDelete.push(key);
                    fileStore.delete(key);
                    cursor.continue();
                } else {
                    // After all files are collected, delete metadata with matching keys
                    for (const key of keysToDelete) {
                        metadataStore.delete(key);
                    }
                }
            };

            cursorRequest.onerror = () => {
                resolve(
                    projectFailure(
                        CommonErrorCode.Unknown,
                        `Failed to delete project files: ${cursorRequest.error?.message || "Unknown error"}`
                    )
                );
            };

            transaction.oncomplete = () => resolve(success(undefined));
            transaction.onerror = () => {
                resolve(
                    projectFailure(
                        CommonErrorCode.Unknown,
                        `Failed to delete project: ${transaction.error?.message || "Unknown error"}`
                    )
                );
            };
        });
    }

    /**
     * Gets metadata from the database.
     */
    private async getMetadata(projectId: string, path: string): Promise<object | null> {
        const db = await this.ensureDB();
        const key = this.makeFileKey(projectId, path);

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["metadata"], "readonly");
            const store = transaction.objectStore("metadata");
            const request = store.get(key);

            request.onsuccess = () => resolve(request.result || null);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Sets metadata in the database.
     */
    private async setMetadata(projectId: string, path: string, metadata: object): Promise<void> {
        const db = await this.ensureDB();
        const key = this.makeFileKey(projectId, path);

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["metadata"], "readwrite");
            const store = transaction.objectStore("metadata");
            const request = store.put(metadata, key);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Deletes metadata from the database.
     */
    private async deleteMetadata(projectId: string, path: string): Promise<void> {
        const db = await this.ensureDB();
        const key = this.makeFileKey(projectId, path);

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["metadata"], "readwrite");
            const store = transaction.objectStore("metadata");
            const request = store.delete(key);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async readMetadata(projectId: string, path: string): Promise<ApiResult<object, FileSystemError>> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            return fileSystemFailure(FileSystemErrorCode.FileNotFound, `File not found: ${path}`);
        }

        const metadata = await this.getMetadata(projectId, path);
        return success(metadata || {});
    }

    async writeMetadata(projectId: string, path: string, metadata: object): Promise<ApiResult<void, FileSystemError>> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            return fileSystemFailure(FileSystemErrorCode.FileNotFound, `File not found: ${path}`);
        }

        await this.setMetadata(projectId, path, metadata);
        return success(undefined);
    }
}
