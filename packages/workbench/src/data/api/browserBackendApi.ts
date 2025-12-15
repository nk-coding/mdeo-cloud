import type {
    IFileDeleteOptions,
    IFileOverwriteOptions,
    IFileWriteOptions,
    FileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import {
    createFileSystemProviderError,
    FileSystemProviderErrorCode,
    FileType as VSCodeFileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { BackendApi } from "./backendApi";
import type { Project } from "../project/project";

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
    private dbVersion = 1;
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
            throw createFileSystemProviderError(
                "Failed to initialize database",
                FileSystemProviderErrorCode.Unavailable
            );
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
            throw createFileSystemProviderError(
                `Not a directory: ${dirPath}`,
                FileSystemProviderErrorCode.FileNotADirectory
            );
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

    async readFile(projectId: string, path: string): Promise<Uint8Array> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            throw createFileSystemProviderError(`File not found: ${path}`, FileSystemProviderErrorCode.FileNotFound);
        }

        if (entry.type === VSCodeFileType.Directory) {
            throw createFileSystemProviderError(
                `Is a directory: ${path}`,
                FileSystemProviderErrorCode.FileIsADirectory
            );
        }

        return entry.content || new Uint8Array(0);
    }

    async writeFile(projectId: string, path: string, content: Uint8Array, opts: IFileWriteOptions): Promise<void> {
        const entry = await this.getFileEntry(projectId, path);

        if (entry) {
            if (entry.type === VSCodeFileType.Directory) {
                throw createFileSystemProviderError(
                    `Is a directory: ${path}`,
                    FileSystemProviderErrorCode.FileIsADirectory
                );
            }
            if (!opts.overwrite) {
                throw createFileSystemProviderError(
                    `File already exists: ${path}`,
                    FileSystemProviderErrorCode.FileExists
                );
            }
        } else {
            if (!opts.create) {
                throw createFileSystemProviderError(
                    `File not found: ${path}`,
                    FileSystemProviderErrorCode.FileNotFound
                );
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
    }

    async mkdir(projectId: string, path: string): Promise<void> {
        const entry = await this.getFileEntry(projectId, path);

        if (entry) {
            if (entry.type === VSCodeFileType.Directory) {
                // Directory already exists, this is OK
                return;
            }
            throw createFileSystemProviderError(`File already exists: ${path}`, FileSystemProviderErrorCode.FileExists);
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
    }

    async readdir(projectId: string, path: string): Promise<[string, FileType][]> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            throw createFileSystemProviderError(
                `Directory not found: ${path}`,
                FileSystemProviderErrorCode.FileNotFound
            );
        }

        if (entry.type !== VSCodeFileType.Directory) {
            throw createFileSystemProviderError(
                `Not a directory: ${path}`,
                FileSystemProviderErrorCode.FileNotADirectory
            );
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
        return result;
    }

    async stat(projectId: string, path: string): Promise<FileType> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            throw createFileSystemProviderError(
                `File or directory not found: ${path}`,
                FileSystemProviderErrorCode.FileNotFound
            );
        }

        return entry.type;
    }

    async delete(projectId: string, path: string, opts: IFileDeleteOptions): Promise<void> {
        const entry = await this.getFileEntry(projectId, path);

        if (!entry) {
            throw createFileSystemProviderError(
                `File or directory not found: ${path}`,
                FileSystemProviderErrorCode.FileNotFound
            );
        }

        if (entry.type === VSCodeFileType.Directory) {
            const children = entry.children || [];
            if (children.length > 0 && !opts.recursive) {
                throw createFileSystemProviderError(
                    `Directory not empty: ${path}`,
                    FileSystemProviderErrorCode.Unknown
                );
            }

            // Delete all children recursively
            for (const child of children) {
                const childPath = path ? `${path}/${child}` : child;
                await this.delete(projectId, childPath, { ...opts, recursive: true });
            }
        }

        // Remove from parent directory
        const parentPath = this.getParentPath(path);
        if (parentPath !== null) {
            const basename = this.getBasename(path);
            await this.removeChildFromDirectory(projectId, parentPath, basename);
        }

        await this.deleteFileEntry(projectId, path);
    }

    async rename(projectId: string, from: string, to: string, opts: IFileOverwriteOptions): Promise<void> {
        const fromEntry = await this.getFileEntry(projectId, from);

        if (!fromEntry) {
            throw createFileSystemProviderError(
                `File or directory not found: ${from}`,
                FileSystemProviderErrorCode.FileNotFound
            );
        }

        const toEntry = await this.getFileEntry(projectId, to);
        if (toEntry && !opts.overwrite) {
            throw createFileSystemProviderError(
                `File or directory already exists: ${to}`,
                FileSystemProviderErrorCode.FileExists
            );
        }

        // If destination exists and overwrite is allowed, delete it first
        if (toEntry) {
            await this.delete(projectId, to, { recursive: true, useTrash: false, atomic: false });
        }

        // Ensure parent directories exist for destination
        await this.ensureParentDirectories(projectId, to);

        // Handle directory rename/move
        if (fromEntry.type === VSCodeFileType.Directory) {
            const children = fromEntry.children || [];

            // Create new directory
            await this.mkdir(projectId, to);

            // Move all children
            for (const child of children) {
                const fromChildPath = from ? `${from}/${child}` : child;
                const toChildPath = to ? `${to}/${child}` : child;
                await this.rename(projectId, fromChildPath, toChildPath, { overwrite: true });
            }
        } else {
            // Move file
            await this.setFileEntry(projectId, to, fromEntry);

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
    }

    async getProjects(): Promise<Project[]> {
        const db = await this.ensureDB();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["projects"], "readonly");
            const store = transaction.objectStore("projects");
            const request = store.getAll();

            request.onsuccess = () => {
                const projects = request.result as ProjectData[];
                resolve(projects.map((p) => ({ id: p.id, name: p.name })));
            };
            request.onerror = () => reject(request.error);
        });
    }

    async createProject(name: string): Promise<string> {
        const db = await this.ensureDB();
        const id = `project-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["projects", "files"], "readwrite");
            const projectStore = transaction.objectStore("projects");
            const fileStore = transaction.objectStore("files");

            const projectData: ProjectData = { id, name };
            const projectRequest = projectStore.add(projectData);

            projectRequest.onerror = () => reject(projectRequest.error);

            // Create root directory for the project
            const rootKey = this.makeFileKey(id, "");
            const rootEntry: FileEntry = {
                type: VSCodeFileType.Directory,
                children: []
            };
            const fileRequest = fileStore.put({ ...rootEntry, projectId: id }, rootKey);

            fileRequest.onerror = () => reject(fileRequest.error);

            transaction.oncomplete = () => resolve(id);
            transaction.onerror = () => reject(transaction.error);
        });
    }

    async updateProject(projectId: string, updates: { name?: string }): Promise<void> {
        const db = await this.ensureDB();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["projects"], "readwrite");
            const store = transaction.objectStore("projects");
            const getRequest = store.get(projectId);

            getRequest.onsuccess = () => {
                const project = getRequest.result as ProjectData | undefined;
                if (!project) {
                    reject(
                        createFileSystemProviderError(
                            `Project not found: ${projectId}`,
                            FileSystemProviderErrorCode.FileNotFound
                        )
                    );
                    return;
                }

                const updatedProject: ProjectData = {
                    ...project,
                    ...updates
                };

                const putRequest = store.put(updatedProject);
                putRequest.onsuccess = () => resolve();
                putRequest.onerror = () => reject(putRequest.error);
            };

            getRequest.onerror = () => reject(getRequest.error);
        });
    }

    async deleteProject(projectId: string): Promise<void> {
        const db = await this.ensureDB();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(["projects", "files"], "readwrite");
            const projectStore = transaction.objectStore("projects");
            const fileStore = transaction.objectStore("files");

            // Delete project
            const deleteProjectRequest = projectStore.delete(projectId);
            deleteProjectRequest.onerror = () => reject(deleteProjectRequest.error);

            // Delete all files for this project
            const index = fileStore.index("projectId");
            const range = IDBKeyRange.only(projectId);
            const cursorRequest = index.openCursor(range);

            cursorRequest.onsuccess = (event) => {
                const cursor = (event.target as IDBRequest<IDBCursorWithValue>).result;
                if (cursor) {
                    fileStore.delete(cursor.primaryKey);
                    cursor.continue();
                }
            };

            cursorRequest.onerror = () => reject(cursorRequest.error);

            transaction.oncomplete = () => resolve();
            transaction.onerror = () => reject(transaction.error);
        });
    }
}
