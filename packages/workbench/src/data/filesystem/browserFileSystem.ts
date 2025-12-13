import { FileSystem } from "./fileSystem.js";
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
import { reactive } from "vue";

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
     * Text content (files only)
     */
    content?: string;
}

/**
 * File system implementation using IndexedDB for browser-based persistent storage.
 * Provides a hierarchical file system that persists data across browser sessions.
 *
 * All data is loaded into reactive objects on initialization for optimal performance.
 * All operations update both the reactive state and persist to IndexedDB.
 *
 * @example
 * ```typescript
 * const fileSystem = new BrowserFileSystem();
 * await fileSystem.initialize();
 *
 * // Get reactive root folder - automatically updates when changed
 * const rootFolder = await fileSystem.getRootFolder();
 * const rootEntries = rootFolder.children; // Direct access to children array
 *
 * // All returned objects are reactive and will update UI automatically
 * ```
 */
export class BrowserFileSystem extends FileSystem {
    private db: IDBDatabase | null = null;
    private readonly dbName = "FileSystemDB";
    private readonly dbVersion = 1;
    private readonly storeName = "nodes";

    // Reactive state - entire filesystem loaded in memory
    private nodes = reactive(new Map<string, FileSystemNode>());
    private rootFolder: Folder | null = null;

    // Initialization promise that resolves when the filesystem is ready
    private readonly initialized: Promise<void>;

    constructor() {
        super();
        this.initialized = this.initializeInternal();
    }

    async initialize(): Promise<void> {
        await this.initialized;
    }

    private async initializeInternal(): Promise<void> {
        // Initialize IndexedDB
        this.db = await new Promise<IDBDatabase>((resolve, reject) => {
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
                }
            };
        });

        // Load entire filesystem into memory
        await this.loadFileSystem();
    }

    /**
     * Load entire filesystem from IndexedDB into reactive memory.
     */
    private async loadFileSystem(): Promise<void> {
        const allStoredNodes = await this.getAllStoredNodes();

        // Clear existing state
        this.nodes.clear();
        this.rootFolder = null;

        // Convert and store all nodes first (without parent/children relationships)
        for (const storedNode of allStoredNodes) {
            const node = this.convertFromIndexedDB(storedNode);
            this.nodes.set(node.id, node);

            // Find root folder
            if (node.type === FileType.FOLDER && storedNode.parentId === null) {
                this.rootFolder = node as Folder;
            }
        }

        // Create root folder if it doesn't exist
        if (!this.rootFolder) {
            await this.createRootFolder();
            return;
        }

        // Build parent-child relationships and paths
        this.buildRelationshipsAndPaths(allStoredNodes);
    }

    /**
     * Build parent-child relationships and compute paths for all nodes.
     */
    private buildRelationshipsAndPaths(storedNodes: IndexedDBNode[]): void {
        // Group nodes by parent ID for efficient lookup
        const nodesByParent = new Map<string | null, IndexedDBNode[]>();
        for (const storedNode of storedNodes) {
            const parentId = storedNode.parentId;
            if (!nodesByParent.has(parentId)) {
                nodesByParent.set(parentId, []);
            }
            nodesByParent.get(parentId)!.push(storedNode);
        }

        // Recursively build relationships starting from root
        this.buildNodeRelationships(this.rootFolder!, nodesByParent, "/");
    }

    /**
     * Recursively build parent-child relationships and paths for a node and its descendants.
     */
    private buildNodeRelationships(
        node: FileSystemNode, 
        nodesByParent: Map<string | null, IndexedDBNode[]>, 
        path: string
    ): void {
        node.path = path;
        
        if (node.type === FileType.FOLDER) {
            const folder = node as Folder;
            const childStoredNodes = nodesByParent.get(node.id) || [];
            
            folder.children = childStoredNodes
                .map(childStored => this.nodes.get(childStored.id))
                .filter(child => child !== undefined) as FileSystemNode[];
            
            // Set parent references and recursively process children
            for (const child of folder.children) {
                child.parent = folder;
                const childPath = path === "/" ? `/${child.name}` : `${path}/${child.name}`;
                this.buildNodeRelationships(child, nodesByParent, childPath);
            }
        }
    }

    /**
     * Create the root folder if it doesn't exist.
     */
    private async createRootFolder(): Promise<void> {
        const rootId = this.generateId();
        const rootFolder: Folder = reactive({
            id: rootId,
            name: "",
            type: FileType.FOLDER,
            parent: null,
            path: "/",
            children: []
        });

        this.nodes.set(rootId, rootFolder);
        this.rootFolder = rootFolder;

        // Persist to IndexedDB
        await this.storeNode(this.convertToIndexedDB(rootFolder));
    }

    /**
     * Get the database instance, awaiting initialization if needed.
     * @returns The IndexedDB database instance
     */
    private async getDatabase(): Promise<IDBDatabase> {
        if (!this.db) {
            throw new Error("Database not initialized");
        }
        return this.db;
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
        // Note: parent, path, and children will be set during relationship building
        const base = {
            id: node.id,
            name: node.name,
            type: node.type,
            parent: null as Folder | null,
            path: "" // Will be computed during relationship building
        };

        if (node.type === FileType.FILE) {
            return reactive({
                ...base,
                type: FileType.FILE,
                content: node.content || ""
            }) as File;
        } else {
            return reactive({
                ...base,
                type: FileType.FOLDER,
                children: [] // Will be populated during relationship building
            }) as Folder;
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
            parentId: node.parent?.id || null
        };

        if (node.type === FileType.FILE) {
            const file = node as File;
            base.content = file.content;
        }

        return base;
    }

    async createFile(options: CreateFileOptions): Promise<File> {
        this.validateName(options.name);

        const parentId = options.parentId || this.rootFolder!.id;
        if (!parentId) throw new Error("No root folder found");

        const parent = this.nodes.get(parentId);
        if (!parent || parent.type !== FileType.FOLDER) {
            throw new Error("Parent folder not found");
        }

        const path = this.buildPath(parent.path === "/" ? null : parent.path, options.name);

        // Check if path already exists
        for (const node of this.nodes.values()) {
            if (node.path === path) {
                throw new Error(`File or folder already exists at path: ${path}`);
            }
        }

        const file = reactive({
            id: this.generateId(),
            name: options.name,
            type: FileType.FILE,
            parent: parent as Folder,
            path,
            content: options.content || ""
        }) as File;

        // Update reactive state
        this.nodes.set(file.id, file);
        (parent as Folder).children.push(file);

        // Persist to IndexedDB
        await this.storeNode(this.convertToIndexedDB(file));
        await this.storeNode(this.convertToIndexedDB(parent));

        this.emitEvent({ type: "created", node: file });
        return file;
    }

    async createFolder(options: CreateFolderOptions): Promise<Folder> {
        this.validateName(options.name);

        const parentId = options.parentId || this.rootFolder!.id;
        if (!parentId) throw new Error("No root folder found");

        const parent = this.nodes.get(parentId);
        if (!parent || parent.type !== FileType.FOLDER) {
            throw new Error("Parent folder not found");
        }

        const path = this.buildPath(parent.path === "/" ? null : parent.path, options.name);

        // Check if a child with the same name already exists in the parent folder
        const parentFolder = parent as Folder;
        if (parentFolder.children.some(child => child.name === options.name)) {
            throw new Error(`File or folder with name "${options.name}" already exists in this folder`);
        }

        const folder = reactive({
            id: this.generateId(),
            name: options.name,
            type: FileType.FOLDER,
            parent: parent as Folder,
            path,
            children: []
        }) as Folder;

        // Update reactive state
        this.nodes.set(folder.id, folder);
        (parent as Folder).children.push(folder);

        // Persist to IndexedDB
        await this.storeNode(this.convertToIndexedDB(folder));
        await this.storeNode(this.convertToIndexedDB(parent));

        this.emitEvent({ type: "created", node: folder });
        return folder;
    }

    async getNode(id: string): Promise<FileSystemNode | null> {
        return this.nodes.get(id) || null;
    }

    async getNodeByPath(path: string): Promise<FileSystemNode | null> {
        for (const node of this.nodes.values()) {
            if (node.path === path) {
                return node;
            }
        }
        return null;
    }

    async updateFile(id: string, options: UpdateFileOptions): Promise<File> {
        const file = this.nodes.get(id) as File;
        if (!file || file.type !== FileType.FILE) {
            throw new Error(`File with id ${id} not found`);
        }

        if (options.name && options.name !== file.name) {
            this.validateName(options.name);

            // Check if a sibling with the same name already exists
            if (file.parent) {
                const siblings = file.parent.children.filter(child => child.id !== id);
                if (siblings.some(sibling => sibling.name === options.name)) {
                    throw new Error(`File or folder with name "${options.name}" already exists in this folder`);
                }
            }

            const parent = file.parent;
            const parentPath = parent ? (parent.path === "/" ? null : parent.path) : null;
            const newPath = this.buildPath(parentPath, options.name);

            file.name = options.name;
            file.path = newPath;
        }

        if (options.content !== undefined) {
            file.content = options.content;
        }

        // Persist to IndexedDB
        await this.storeNode(this.convertToIndexedDB(file));

        this.emitEvent({ type: "updated", node: file });
        return file;
    }

    async deleteNode(id: string): Promise<void> {
        const node = this.nodes.get(id);
        if (!node) {
            throw new Error(`Node with id ${id} not found`);
        }

        // Recursively delete children if it's a folder
        if (node.type === FileType.FOLDER) {
            const folder = node as Folder;
            for (const child of [...folder.children]) {
                await this.deleteNode(child.id);
            }
        }

        // Remove from parent's children list
        if (node.parent) {
            const parent = node.parent;
            const index = parent.children.findIndex((child) => child.id === id);
            if (index !== -1) {
                parent.children.splice(index, 1);
            }
            await this.storeNode(this.convertToIndexedDB(parent));
        }

        // Remove from reactive state
        this.nodes.delete(id);

        // Remove from IndexedDB
        await this.deleteStoredNode(id);

        this.emitEvent({ type: "deleted", node });
    }

    async moveNode(id: string, options: MoveOptions): Promise<FileSystemNode> {
        const node = this.nodes.get(id);
        if (!node) {
            throw new Error(`Node with id ${id} not found`);
        }

        const newParent = options.targetParent || this.rootFolder!;
        if (!newParent || newParent.type !== FileType.FOLDER) {
            throw new Error("Target parent folder not found");
        }

        const newName = options.newName || node.name;
        this.validateName(newName);

        // Check if a child with the same name already exists in the target parent
        if (newParent.children.some(child => child.name === newName && child.id !== id)) {
            throw new Error(`File or folder with name "${newName}" already exists in the target folder`);
        }

        const oldPath = node.path;
        const newPath = this.buildPath(newParent.path === "/" ? null : newParent.path, newName);

        // Remove from current parent
        if (node.parent) {
            const currentParent = node.parent;
            const index = currentParent.children.findIndex((child) => child.id === id);
            if (index !== -1) {
                currentParent.children.splice(index, 1);
            }
            await this.storeNode(this.convertToIndexedDB(currentParent));
        }

        // Update node properties
        node.name = newName;
        node.parent = newParent;
        node.path = newPath;

        // Update descendant paths if it's a folder
        if (node.type === FileType.FOLDER) {
            this.updateDescendantPaths(node as Folder, oldPath, newPath);
        }

        // Add to new parent
        newParent.children.push(node);

        // Persist changes
        await this.storeNode(this.convertToIndexedDB(node));
        await this.storeNode(this.convertToIndexedDB(newParent));

        this.emitEvent({ type: "moved", node, oldPath });
        return node;
    }

    /**
     * Update the paths of all descendants when a folder is moved.
     * @param folder - The moved folder
     * @param oldFolderPath - The old path of the folder
     * @param newFolderPath - The new path of the folder
     */
    private updateDescendantPaths(folder: Folder, oldFolderPath: string, newFolderPath: string): void {
        // Recursively update paths for all descendants
        const updateChildPaths = (parent: Folder, parentPath: string) => {
            for (const child of parent.children) {
                const childPath = parentPath === "/" ? `/${child.name}` : `${parentPath}/${child.name}`;
                child.path = childPath;
                
                if (child.type === FileType.FOLDER) {
                    updateChildPaths(child as Folder, childPath);
                }
            }
        };
        
        updateChildPaths(folder, newFolderPath);
    }

    async getRootFolder(): Promise<Folder> {
        await this.initialize();

        return this.rootFolder!;
    }
}
