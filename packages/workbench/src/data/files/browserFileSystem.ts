import { FileSystem, type FileSystemStats } from "./fileSystem.js";
import type {
    FileSystemNode,
    File,
    Folder,
    CreateFileOptions,
    CreateFolderOptions,
    UpdateFileOptions,
    MoveOptions
} from "./file.js";
import { FileType } from "./file.js";

/**
 * Internal representation of a node as stored in IndexedDB.
 * Dates are serialized as ISO strings for storage compatibility.
 */
interface IndexedDBNode {
    /**
     * Unique identifier for the node
     */
    id: string;
    /**
     * Display name of the node
     */
    name: string;
    /**
     * Type of the node (file or folder)
     */
    type: FileType;
    /**
     * ID of parent folder, null for root
     */
    parentId: string | null;
    /**
     * Full path from root to this node
     */
    path: string;
    /**
     * Creation timestamp as ISO string
     */
    createdAt: string;
    /**
     * Last modification timestamp as ISO string
     */
    modifiedAt: string;
    /**
     * Text content (files only)
     */
    content?: string;
    /**
     * MIME type (files only)
     */
    mimeType?: string;
    /**
     * Array of child node IDs (folders only)
     */
    children?: string[];
    /**
     * Size in bytes (files only)
     */
    size?: number;
}

/**
 * File system implementation using IndexedDB for browser-based persistent storage.
 * Provides a hierarchical file system that persists data across browser sessions.
 */
export class BrowserFileSystem extends FileSystem {
    private dbPromise: Promise<IDBDatabase> | null = null;
    private readonly dbName = "FileSystemDB";
    private readonly dbVersion = 1;
    private readonly storeName = "nodes";
    private rootFolderId: string | null = null;

    async initialize(): Promise<void> {
        if (!this.dbPromise) {
            this.dbPromise = new Promise((resolve, reject) => {
                const request = indexedDB.open(this.dbName, this.dbVersion);

                request.onerror = () => {
                    reject(new Error("Failed to open IndexedDB"));
                };

                request.onsuccess = () => {
                    resolve(request.result);
                };

                request.onupgradeneeded = (event) => {
                    const db = (event.target as IDBOpenDBRequest).result;

                    if (!db.objectStoreNames.contains(this.storeName)) {
                        const store = db.createObjectStore(this.storeName, { keyPath: "id" });
                        store.createIndex("parentId", "parentId", { unique: false });
                        store.createIndex("path", "path", { unique: true });
                    }
                };
            });
        }

        await this.dbPromise;
        await this.ensureRootFolder();
    }

    /**
     * Get the database instance, awaiting initialization if needed.
     * @returns The IndexedDB database instance
     */
    private async getDatabase(): Promise<IDBDatabase> {
        if (!this.dbPromise) {
            await this.initialize();
        }
        return this.dbPromise!;
    }

    /**
     * Ensure the root folder exists in the database.
     * Creates it if it doesn't exist.
     */
    private async ensureRootFolder(): Promise<void> {
        const existingRoot = await this.getNodeByPath("/");
        if (existingRoot) {
            this.rootFolderId = existingRoot.id;
            return;
        }

        const rootId = this.generateId();
        const rootFolder: IndexedDBNode = {
            id: rootId,
            name: "",
            type: FileType.FOLDER,
            parentId: null,
            path: "/",
            createdAt: new Date().toISOString(),
            modifiedAt: new Date().toISOString(),
            children: []
        };

        await this.storeNode(rootFolder);
        this.rootFolderId = rootId;
    }

    /**
     * Store a node in the IndexedDB database.
     * @param node - The node to store
     */
    private async storeNode(node: IndexedDBNode): Promise<void> {
        const db = await this.getDatabase();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction([this.storeName], "readwrite");
            const store = transaction.objectStore(this.storeName);
            const request = store.put(node);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(new Error("Failed to store node"));
        });
    }

    /**
     * Retrieve a node from IndexedDB by its ID.
     * @param id - The unique identifier of the node
     * @returns The stored node or null if not found
     */
    private async getStoredNode(id: string): Promise<IndexedDBNode | null> {
        const db = await this.getDatabase();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction([this.storeName], "readonly");
            const store = transaction.objectStore(this.storeName);
            const request = store.get(id);

            request.onsuccess = () => {
                resolve(request.result || null);
            };
            request.onerror = () => reject(new Error("Failed to get node"));
        });
    }

    /**
     * Retrieve a node from IndexedDB by its path.
     * @param path - The full path of the node
     * @returns The stored node or null if not found
     */
    private async getStoredNodeByPath(path: string): Promise<IndexedDBNode | null> {
        const db = await this.getDatabase();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction([this.storeName], "readonly");
            const store = transaction.objectStore(this.storeName);
            const index = store.index("path");
            const request = index.get(path);

            request.onsuccess = () => {
                resolve(request.result || null);
            };
            request.onerror = () => reject(new Error("Failed to get node by path"));
        });
    }

    /**
     * Delete a node from IndexedDB.
     * @param id - The unique identifier of the node to delete
     */
    private async deleteStoredNode(id: string): Promise<void> {
        const db = await this.getDatabase();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction([this.storeName], "readwrite");
            const store = transaction.objectStore(this.storeName);
            const request = store.delete(id);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(new Error("Failed to delete node"));
        });
    }

    /**
     * Retrieve all nodes from IndexedDB.
     * @returns Array of all stored nodes
     */
    private async getAllStoredNodes(): Promise<IndexedDBNode[]> {
        const db = await this.getDatabase();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction([this.storeName], "readonly");
            const store = transaction.objectStore(this.storeName);
            const request = store.getAll();

            request.onsuccess = () => {
                resolve(request.result || []);
            };
            request.onerror = () => reject(new Error("Failed to get all nodes"));
        });
    }

    /**
     * Convert an IndexedDB node to a FileSystemNode.
     * @param node - The stored node to convert
     * @returns Converted file system node
     */
    private convertFromIndexedDB(node: IndexedDBNode): FileSystemNode {
        const base = {
            id: node.id,
            name: node.name,
            type: node.type,
            parentId: node.parentId,
            path: node.path,
            createdAt: new Date(node.createdAt),
            modifiedAt: new Date(node.modifiedAt)
        };

        if (node.type === FileType.FILE) {
            return {
                ...base,
                type: FileType.FILE,
                content: node.content || "",
                mimeType: node.mimeType,
                size: node.size
            } as File;
        } else {
            return {
                ...base,
                type: FileType.FOLDER,
                children: node.children || []
            } as Folder;
        }
    }

    /**
     * Convert a FileSystemNode to an IndexedDB node for storage.
     * @param node - The file system node to convert
     * @returns Converted IndexedDB node
     */
    private convertToIndexedDB(node: FileSystemNode): IndexedDBNode {
        const base: IndexedDBNode = {
            id: node.id,
            name: node.name,
            type: node.type,
            parentId: node.parentId,
            path: node.path,
            createdAt: node.createdAt.toISOString(),
            modifiedAt: node.modifiedAt.toISOString()
        };

        if (node.type === FileType.FILE) {
            const file = node as File;
            base.content = file.content;
            base.mimeType = file.mimeType;
            base.size = file.size || file.content.length;
        } else {
            const folder = node as Folder;
            base.children = folder.children;
        }

        return base;
    }

    async createFile(options: CreateFileOptions): Promise<File> {
        this.validateName(options.name);

        const parentId = options.parentId || this.rootFolderId || (await this.getDatabase(), this.rootFolderId);
        if (!parentId) throw new Error("No root folder found");

        const parent = await this.getStoredNode(parentId);
        if (!parent || parent.type !== FileType.FOLDER) {
            throw new Error("Parent folder not found");
        }

        const path = this.buildPath(parent.path === "/" ? null : parent.path, options.name);

        const existing = await this.getStoredNodeByPath(path);
        if (existing) {
            throw new Error(`File or folder already exists at path: ${path}`);
        }

        const file: File = {
            id: this.generateId(),
            name: options.name,
            type: FileType.FILE,
            parentId,
            path,
            createdAt: new Date(),
            modifiedAt: new Date(),
            content: options.content || "",
            mimeType: options.mimeType,
            size: (options.content || "").length
        };

        await this.storeNode(this.convertToIndexedDB(file));

        parent.children = parent.children || [];
        parent.children.push(file.id);
        parent.modifiedAt = new Date().toISOString();
        await this.storeNode(parent);

        this.emitEvent({ type: "created", node: file });
        return file;
    }

    async createFolder(options: CreateFolderOptions): Promise<Folder> {
        this.validateName(options.name);

        const parentId = options.parentId || this.rootFolderId || (await this.getDatabase(), this.rootFolderId);
        if (!parentId) throw new Error("No root folder found");

        const parent = await this.getStoredNode(parentId);
        if (!parent || parent.type !== FileType.FOLDER) {
            throw new Error("Parent folder not found");
        }

        const path = this.buildPath(parent.path === "/" ? null : parent.path, options.name);

        const existing = await this.getStoredNodeByPath(path);
        if (existing) {
            throw new Error(`File or folder already exists at path: ${path}`);
        }

        const folder: Folder = {
            id: this.generateId(),
            name: options.name,
            type: FileType.FOLDER,
            parentId,
            path,
            createdAt: new Date(),
            modifiedAt: new Date(),
            children: []
        };

        await this.storeNode(this.convertToIndexedDB(folder));

        parent.children = parent.children || [];
        parent.children.push(folder.id);
        parent.modifiedAt = new Date().toISOString();
        await this.storeNode(parent);

        this.emitEvent({ type: "created", node: folder });
        return folder;
    }

    async getNode(id: string): Promise<FileSystemNode | null> {
        const stored = await this.getStoredNode(id);
        return stored ? this.convertFromIndexedDB(stored) : null;
    }

    async getNodeByPath(path: string): Promise<FileSystemNode | null> {
        const stored = await this.getStoredNodeByPath(path);
        return stored ? this.convertFromIndexedDB(stored) : null;
    }

    async updateFile(id: string, options: UpdateFileOptions): Promise<File> {
        const stored = await this.getStoredNode(id);
        if (!stored || stored.type !== FileType.FILE) {
            throw new Error(`File with id ${id} not found`);
        }

        if (options.name && options.name !== stored.name) {
            this.validateName(options.name);

            const parent = stored.parentId ? await this.getStoredNode(stored.parentId) : null;
            const parentPath = parent ? (parent.path === "/" ? null : parent.path) : null;
            const newPath = this.buildPath(parentPath, options.name);

            const existing = await this.getStoredNodeByPath(newPath);
            if (existing && existing.id !== id) {
                throw new Error(`File or folder already exists at path: ${newPath}`);
            }

            stored.name = options.name;
            stored.path = newPath;
        }

        if (options.content !== undefined) {
            stored.content = options.content;
            stored.size = options.content.length;
        }

        stored.modifiedAt = new Date().toISOString();
        await this.storeNode(stored);

        const updatedFile = this.convertFromIndexedDB(stored) as File;
        this.emitEvent({ type: "updated", node: updatedFile });

        return updatedFile;
    }

    async deleteNode(id: string): Promise<void> {
        const stored = await this.getStoredNode(id);
        if (!stored) {
            throw new Error(`Node with id ${id} not found`);
        }

        if (stored.type === FileType.FOLDER && stored.children) {
            for (const childId of stored.children) {
                await this.deleteNode(childId);
            }
        }

        if (stored.parentId) {
            const parent = await this.getStoredNode(stored.parentId);
            if (parent && parent.children) {
                parent.children = parent.children.filter((childId) => childId !== id);
                parent.modifiedAt = new Date().toISOString();
                await this.storeNode(parent);
            }
        }

        const node = this.convertFromIndexedDB(stored);
        await this.deleteStoredNode(id);

        this.emitEvent({ type: "deleted", node });
    }

    async moveNode(id: string, options: MoveOptions): Promise<FileSystemNode> {
        const stored = await this.getStoredNode(id);
        if (!stored) {
            throw new Error(`Node with id ${id} not found`);
        }

        const targetParentId =
            options.targetParentId || this.rootFolderId || (await this.getDatabase(), this.rootFolderId);
        if (!targetParentId) throw new Error("No root folder found");

        if (stored.parentId) {
            const currentParent = await this.getStoredNode(stored.parentId);
            if (currentParent && currentParent.children) {
                currentParent.children = currentParent.children.filter((childId) => childId !== id);
                currentParent.modifiedAt = new Date().toISOString();
                await this.storeNode(currentParent);
            }
        }

        const newParent = await this.getStoredNode(targetParentId);
        if (!newParent || newParent.type !== FileType.FOLDER) {
            throw new Error("Target parent folder not found");
        }

        const newName = options.newName || stored.name;
        this.validateName(newName);

        const newPath = this.buildPath(newParent.path === "/" ? null : newParent.path, newName);

        const existing = await this.getStoredNodeByPath(newPath);
        if (existing && existing.id !== id) {
            throw new Error(`File or folder already exists at path: ${newPath}`);
        }

        const oldPath = stored.path;

        stored.name = newName;
        stored.parentId = targetParentId;
        stored.path = newPath;
        stored.modifiedAt = new Date().toISOString();

        if (stored.type === FileType.FOLDER) {
            await this.updateDescendantPaths(id, oldPath, newPath);
        }

        await this.storeNode(stored);

        newParent.children = newParent.children || [];
        newParent.children.push(id);
        newParent.modifiedAt = new Date().toISOString();
        await this.storeNode(newParent);

        const movedNode = this.convertFromIndexedDB(stored);
        this.emitEvent({ type: "moved", node: movedNode, oldPath });

        return movedNode;
    }

    /**
     * Update the paths of all descendants when a folder is moved.
     * @param folderId - The ID of the moved folder
     * @param oldFolderPath - The old path of the folder
     * @param newFolderPath - The new path of the folder
     */
    private async updateDescendantPaths(folderId: string, oldFolderPath: string, newFolderPath: string): Promise<void> {
        const allNodes = await this.getAllStoredNodes();

        for (const node of allNodes) {
            if (node.path.startsWith(oldFolderPath + "/")) {
                const relativePath = node.path.substring(oldFolderPath.length + 1);
                node.path = `${newFolderPath}/${relativePath}`;
                node.modifiedAt = new Date().toISOString();
                await this.storeNode(node);
            }
        }
    }

    async listChildren(folderId: string): Promise<FileSystemNode[]> {
        const stored = await this.getStoredNode(folderId);
        if (!stored || stored.type !== FileType.FOLDER) {
            throw new Error(`Folder with id ${folderId} not found`);
        }

        const children: FileSystemNode[] = [];
        if (stored.children) {
            for (const childId of stored.children) {
                const child = await this.getStoredNode(childId);
                if (child) {
                    children.push(this.convertFromIndexedDB(child));
                }
            }
        }

        return children.sort((a, b) => {
            if (a.type !== b.type) {
                return a.type === FileType.FOLDER ? -1 : 1;
            }
            return a.name.localeCompare(b.name);
        });
    }

    async getRootFolder(): Promise<Folder> {
        if (!this.rootFolderId) {
            await this.getDatabase(); // This will trigger initialization if needed
        }

        if (!this.rootFolderId) {
            throw new Error("Root folder not found after initialization");
        }

        const root = await this.getNode(this.rootFolderId);
        if (!root || root.type !== FileType.FOLDER) {
            throw new Error("Root folder not found");
        }

        return root as Folder;
    }

    async getStats(): Promise<FileSystemStats> {
        const allNodes = await this.getAllStoredNodes();

        let totalFiles = 0;
        let totalFolders = 0;
        let totalSize = 0;

        for (const node of allNodes) {
            if (node.type === FileType.FILE) {
                totalFiles++;
                totalSize += node.size || 0;
            } else {
                totalFolders++;
            }
        }

        return { totalFiles, totalFolders, totalSize };
    }
}
