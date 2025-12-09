import type {
    FileSystemNode,
    File,
    Folder,
    CreateFileOptions,
    CreateFolderOptions,
    UpdateFileOptions,
    MoveOptions,
    FileSystemEvent,
    FileSystemEventListener
} from "./file.js";
import { FileType } from "./file.js";

/**
 * Statistics about the file system contents.
 */
export interface FileSystemStats {
    /**
     * Total number of files
     */
    totalFiles: number;
    /**
     * Total number of folders
     */
    totalFolders: number;
    /**
     * Total size of all files in bytes
     */
    totalSize: number;
}

/**
 * Abstract base class for file system implementations.
 * Provides common functionality and defines the interface for concrete implementations.
 */
export abstract class FileSystem {
    private eventListeners: FileSystemEventListener[] = [];

    /**
     * Initialize the file system. Must be called before using other methods.
     */
    abstract initialize(): Promise<void>;

    /**
     * Create a new file in the file system.
     * @param options - Configuration for the new file
     * @returns Promise resolving to the created file
     */
    abstract createFile(options: CreateFileOptions): Promise<File>;

    /**
     * Create a new folder in the file system.
     * @param options - Configuration for the new folder
     * @returns Promise resolving to the created folder
     */
    abstract createFolder(options: CreateFolderOptions): Promise<Folder>;

    /**
     * Get a node by its unique ID.
     * @param id - The unique identifier of the node
     * @returns Promise resolving to the node or null if not found
     */
    abstract getNode(id: string): Promise<FileSystemNode | null>;

    /**
     * Get a node by its full path.
     * @param path - The full path to the node
     * @returns Promise resolving to the node or null if not found
     */
    abstract getNodeByPath(path: string): Promise<FileSystemNode | null>;

    /**
     * Update an existing file's content or name.
     * @param id - The unique identifier of the file
     * @param options - Update options
     * @returns Promise resolving to the updated file
     */
    abstract updateFile(id: string, options: UpdateFileOptions): Promise<File>;

    /**
     * Delete a node and all its children (if it's a folder).
     * @param id - The unique identifier of the node to delete
     */
    abstract deleteNode(id: string): Promise<void>;

    /**
     * Move a node to a different location and optionally rename it.
     * @param id - The unique identifier of the node to move
     * @param options - Move options including target location
     * @returns Promise resolving to the moved node
     */
    abstract moveNode(id: string, options: MoveOptions): Promise<FileSystemNode>;

    /**
     * List all children of a folder.
     * @param folderId - The unique identifier of the folder
     * @returns Promise resolving to array of child nodes
     */
    abstract listChildren(folderId: string): Promise<FileSystemNode[]>;

    /**
     * Get the root folder of the file system.
     * @returns Promise resolving to the root folder
     */
    abstract getRootFolder(): Promise<Folder>;

    /**
     * Get statistics about the file system contents.
     * @returns Promise resolving to file system statistics
     */
    abstract getStats(): Promise<FileSystemStats>;

    /**
     * Add an event listener for file system changes.
     * @param listener - Function to call when events occur
     */
    addEventListener(listener: FileSystemEventListener): void {
        this.eventListeners.push(listener);
    }

    /**
     * Remove an event listener.
     * @param listener - The listener function to remove
     */
    removeEventListener(listener: FileSystemEventListener): void {
        const index = this.eventListeners.indexOf(listener);
        if (index > -1) {
            this.eventListeners.splice(index, 1);
        }
    }

    /**
     * Emit an event to all registered listeners.
     * @param event - The event to emit
     */
    protected emitEvent(event: FileSystemEvent): void {
        this.eventListeners.forEach((listener) => listener(event));
    }

    /**
     * Check if a path exists in the file system.
     * @param path - The path to check
     * @returns Promise resolving to true if the path exists
     */
    async exists(path: string): Promise<boolean> {
        const node = await this.getNodeByPath(path);
        return node !== null;
    }

    /**
     * Check if a path points to a file.
     * @param path - The path to check
     * @returns Promise resolving to true if the path is a file
     */
    async isFile(path: string): Promise<boolean> {
        const node = await this.getNodeByPath(path);
        return node?.type === FileType.FILE;
    }

    /**
     * Check if a path points to a folder.
     * @param path - The path to check
     * @returns Promise resolving to true if the path is a folder
     */
    async isFolder(path: string): Promise<boolean> {
        const node = await this.getNodeByPath(path);
        return node?.type === FileType.FOLDER;
    }

    /**
     * Get the content of a file by its ID.
     * @param id - The unique identifier of the file
     * @returns Promise resolving to the file content
     * @throws Error if the node is not a file
     */
    async getFileContent(id: string): Promise<string> {
        const node = await this.getNode(id);
        if (!node || node.type !== FileType.FILE) {
            throw new Error(`File with id ${id} not found`);
        }
        return (node as File).content;
    }

    /**
     * Get all children of a folder by its ID.
     * @param id - The unique identifier of the folder
     * @returns Promise resolving to array of child nodes
     * @throws Error if the node is not a folder
     */
    async getFolderChildren(id: string): Promise<FileSystemNode[]> {
        const node = await this.getNode(id);
        if (!node || node.type !== FileType.FOLDER) {
            throw new Error(`Folder with id ${id} not found`);
        }
        return this.listChildren(id);
    }

    /**
     * Build a full path from parent path and node name.
     * @param parentPath - The parent's path
     * @param name - The node's name
     * @returns The full path
     */
    protected buildPath(parentPath: string | null, name: string): string {
        if (!parentPath || parentPath === "/") {
            return `/${name}`;
        }
        return `${parentPath}/${name}`;
    }

    /**
     * Validate a node name for correctness.
     * @param name - The name to validate
     * @throws Error if the name is invalid
     */
    protected validateName(name: string): void {
        if (!name || name.trim().length === 0) {
            throw new Error("Name cannot be empty");
        }
        if (name.includes("/")) {
            throw new Error("Name cannot contain forward slashes");
        }
        if (name.startsWith(".") && name.length === 1) {
            throw new Error("Invalid name");
        }
    }

    /**
     * Generate a unique identifier for a node.
     * @returns A unique string identifier
     */
    protected generateId(): string {
        return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Get all root entries (direct children of root folder).
     * @returns Array of root-level nodes
     */
    async getRootEntries(): Promise<FileSystemNode[]> {
        const rootFolder = await this.getRootFolder();
        return this.listChildren(rootFolder.id);
    }

    /**
     * Get a node by its ID (alias for getNode).
     * @param id - The unique identifier of the node
     * @returns The node or null if not found
     */
    async getEntry(id: string): Promise<FileSystemNode | null> {
        return this.getNode(id);
    }

    /**
     * Create a file with simple parameters (convenience method).
     * @param name - The name of the file
     * @param content - Initial content of the file
     * @param parentId - ID of parent folder (optional, defaults to root)
     * @returns The created file
     */
    async createFileSimple(name: string, content: string = "", parentId?: string): Promise<File> {
        const rootFolder = await this.getRootFolder();
        const actualParentId = parentId || rootFolder.id;

        const options: CreateFileOptions = {
            name,
            parentId: actualParentId,
            content,
            mimeType: "text/plain"
        };

        return this.createFile(options);
    }

    /**
     * Create a folder with simple parameters (convenience method).
     * @param name - The name of the folder
     * @param parentId - ID of parent folder (optional, defaults to root)
     * @returns The created folder
     */
    async createFolderSimple(name: string, parentId?: string): Promise<Folder> {
        const rootFolder = await this.getRootFolder();
        const actualParentId = parentId || rootFolder.id;

        const options: CreateFolderOptions = {
            name,
            parentId: actualParentId
        };

        return this.createFolder(options);
    }

    /**
     * Rename a node (convenience method).
     * @param id - The unique identifier of the node
     * @param newName - The new name for the node
     * @returns True if successful
     */
    async renameEntry(id: string, newName: string): Promise<boolean> {
        try {
            const node = await this.getNode(id);
            if (!node) return false;

            if (node.type === FileType.FILE) {
                await this.updateFile(id, { name: newName });
            } else {
                // For folders, we need to use moveNode to rename
                await this.moveNode(id, { targetParentId: node.parentId, newName });
            }
            return true;
        } catch {
            return false;
        }
    }

    /**
     * Delete a node (convenience method).
     * @param id - The unique identifier of the node to delete
     * @returns True if successful
     */
    async deleteEntry(id: string): Promise<boolean> {
        try {
            await this.deleteNode(id);
            return true;
        } catch {
            return false;
        }
    }
}
