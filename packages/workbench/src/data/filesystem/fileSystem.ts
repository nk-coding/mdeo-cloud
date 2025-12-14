import type {
    FileSystemNode,
    File,
    Folder,
    CreateFileOptions,
    UpdateFileOptions,
    FileSystemEvent,
    FileSystemEventListener
} from "./file.js";
import { FileType } from "./file.js";
import type { FileSystemAdapter, StoredNode } from "./fileSystemAdapter.js";
import { reactive } from "vue";

/**
 * File system implementation using composition with a storage adapter.
 * Manages reactive file system state and provides high-level operations.
 */
export class FileSystem {
    /**
     * Event listeners for file system changes
     */
    private eventListeners: FileSystemEventListener[] = [];
    /**
     * Reactive map of node ID to file system node (file or folder)
     */
    private nodes = reactive(new Map<string, FileSystemNode>());
    /**
     * Root folder of the file system
     */
    private rootFolder: Folder | null = null;
    /**
     * Promise that resolves when initialization is complete
     */
    private readonly initialized: Promise<void>;

    /**
     * Creates a new file system instance.
     *
     *
     * @param adapter - The storage adapter to use for persistence
     */
    constructor(private readonly adapter: FileSystemAdapter) {
        this.adapter = adapter;
        this.initialized = this.initializeInternal();
    }

    /**
     * Initialize the file system. Must be called before using other methods.
     * This is idempotent and safe to call multiple times.
     */
    async initialize(): Promise<void> {
        await this.initialized;
    }

    /**
     * Internal initialization that sets up the adapter and loads the file system.
     */
    private async initializeInternal(): Promise<void> {
        await this.adapter.initialize();
        await this.loadFileSystem();
    }

    /**
     * Load the entire file system from storage into reactive memory.
     * Creates the root folder if it doesn't exist.
     */
    private async loadFileSystem(): Promise<void> {
        const allStoredNodes = await this.adapter.loadAll();

        this.nodes.clear();
        this.rootFolder = null;

        for (const storedNode of allStoredNodes) {
            const node = this.convertFromStored(storedNode);
            this.nodes.set(node.id, node);

            if (node.type === FileType.FOLDER && storedNode.parentId === null) {
                this.rootFolder = node as Folder;
            }
        }

        if (this.rootFolder == null) {
            await this.createRootFolder();
            return;
        }

        this.buildRelationshipsAndPaths(allStoredNodes);
    }

    /**
     * Build parent-child relationships and compute paths for all nodes.
     *
     * @param storedNodes - Array of all stored nodes to build relationships from
     */
    private buildRelationshipsAndPaths(storedNodes: StoredNode[]): void {
        const nodesByParent = new Map<string | null, StoredNode[]>();
        for (const storedNode of storedNodes) {
            const parentId = storedNode.parentId;
            if (!nodesByParent.has(parentId)) {
                nodesByParent.set(parentId, []);
            }
            nodesByParent.get(parentId)!.push(storedNode);
        }

        this.buildNodeRelationships(this.rootFolder!, nodesByParent, "/");
    }

    /**
     * Recursively build parent-child relationships and paths for a node and its descendants.
     *
     * @param node - The node to process
     * @param nodesByParent - Map of parent IDs to their child nodes
     * @param path - The computed path for this node
     */
    private buildNodeRelationships(
        node: FileSystemNode,
        nodesByParent: Map<string | null, StoredNode[]>,
        path: string
    ): void {
        node.path = path;

        if (node.type === FileType.FOLDER) {
            const folder = node as Folder;
            const childStoredNodes = nodesByParent.get(node.id) ?? [];

            folder.children = childStoredNodes
                .map((childStored) => this.nodes.get(childStored.id))
                .filter((child) => child !== undefined) as FileSystemNode[];

            for (const child of folder.children) {
                child.parent = folder;
                const childPath = path === "/" ? `/${child.name}` : `${path}/${child.name}`;
                this.buildNodeRelationships(child, nodesByParent, childPath);
            }
        }
    }

    /**
     * Create the root folder if it doesn't exist in storage.
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

        await this.adapter.store(this.convertToStored(rootFolder));
    }

    /**
     * Convert a stored node from the adapter to a reactive file system node.
     *
     * @param node - The stored node to convert
     * @returns A reactive file system node (File or Folder)
     */
    private convertFromStored(node: StoredNode): FileSystemNode {
        const base = {
            id: node.id,
            name: node.name,
            type: node.type === "file" ? FileType.FILE : FileType.FOLDER,
            parent: null as Folder | null,
            path: ""
        };

        if (node.type === "file") {
            return reactive<File>({
                ...base,
                type: FileType.FILE,
                content: node.content ?? "",
                fileType: node.fileType!
            });
        } else {
            return reactive<Folder>({
                ...base,
                type: FileType.FOLDER,
                children: []
            });
        }
    }

    /**
     * Convert a file system node to a stored node for the adapter.
     *
     * @param node - The file system node to convert
     * @returns A stored node suitable for persistence
     */
    private convertToStored(node: FileSystemNode): StoredNode {
        const base: StoredNode = {
            id: node.id,
            name: node.name,
            type: node.type === FileType.FILE ? "file" : "folder",
            parentId: node.parent?.id ?? null
        };

        if (node.type === FileType.FILE) {
            const file = node as File;
            base.content = file.content;
            base.fileType = file.fileType;
        }

        return base;
    }

    /**
     * Get the root folder of the file system.
     *
     * @returns The root folder
     */
    async getRootFolder(): Promise<Folder> {
        await this.initialize();
        return this.rootFolder!;
    }

    /**
     * Get a node by its unique ID.
     *
     * @param id - The unique identifier of the node
     * @returns The node if found, null otherwise
     */
    async getNode(id: string): Promise<FileSystemNode | null> {
        return this.nodes.get(id) ?? null;
    }

    /**
     * Create a new file in the file system.
     *
     * @param options - Configuration for the new file including name, plugin, and optional parent ID
     * @returns The created file
     * @throws Error if the name is invalid or a file/folder already exists at the path
     */
    async createFile(options: CreateFileOptions): Promise<File> {
        this.validateName(options.name);

        const parentId = options.parentId ?? this.rootFolder!.id;
        if (parentId == null) {
            throw new Error("No root folder found");
        }

        const parent = this.nodes.get(parentId);
        if (parent == null || parent.type !== FileType.FOLDER) {
            throw new Error("Parent folder not found");
        }

        const path = this.buildPath(parent.path === "/" ? null : parent.path, options.name);

        for (const node of this.nodes.values()) {
            if (node.path === path) {
                throw new Error(`File or folder already exists at path: ${path}`);
            }
        }

        const file = reactive<File>({
            id: this.generateId(),
            name: options.name,
            type: FileType.FILE,
            parent: parent as Folder,
            path,
            content: options.plugin.defaultContent ?? "",
            fileType: options.plugin.id
        });

        this.nodes.set(file.id, file);
        (parent as Folder).children.push(file);

        await this.adapter.store(this.convertToStored(file));
        await this.adapter.store(this.convertToStored(parent));

        this.emitEvent({ type: "created", node: file });
        return file;
    }

    /**
     * Create a new folder in the file system.
     *
     * @param name - The name of the folder to create
     * @param parentId - Optional ID of the parent folder (defaults to root)
     * @returns The created folder
     * @throws Error if the name is invalid or a folder with the same name already exists
     */
    async createFolder(name: string, parentId?: string): Promise<Folder> {
        this.validateName(name);

        const actualParentId = parentId ?? this.rootFolder!.id;
        if (actualParentId == null) {
            throw new Error("No root folder found");
        }

        const parent = this.nodes.get(actualParentId);
        if (parent == null || parent.type !== FileType.FOLDER) {
            throw new Error("Parent folder not found");
        }

        const path = this.buildPath(parent.path === "/" ? null : parent.path, name);

        const parentFolder = parent as Folder;
        if (parentFolder.children.some((child) => child.name === name)) {
            throw new Error(`File or folder with name "${name}" already exists in this folder`);
        }

        const folder = reactive<Folder>({
            id: this.generateId(),
            name,
            type: FileType.FOLDER,
            parent: parent as Folder,
            path,
            children: []
        });

        this.nodes.set(folder.id, folder);
        (parent as Folder).children.push(folder);

        await this.adapter.store(this.convertToStored(folder));
        await this.adapter.store(this.convertToStored(parent));

        this.emitEvent({ type: "created", node: folder });
        return folder;
    }

    /**
     * Update an existing file's content.
     *
     * @param id - The unique identifier of the file
     * @param options - Update options including optional content
     * @returns The updated file
     * @throws Error if the file doesn't exist
     */
    async updateFile(id: string, options: UpdateFileOptions): Promise<File> {
        const file = this.nodes.get(id) as File;
        if (file == null || file.type !== FileType.FILE) {
            throw new Error(`File with id ${id} not found`);
        }

        if (options.content !== undefined) {
            file.content = options.content;
        }

        await this.adapter.store(this.convertToStored(file));

        this.emitEvent({ type: "updated", node: file });
        return file;
    }

    /**
     * Rename a node (file or folder).
     *
     * @param id - The unique identifier of the node
     * @param newName - The new name for the node
     * @returns True if successful, false if the node wasn't found
     */
    async rename(id: string, newName: string): Promise<boolean> {
        try {
            const node = await this.getNode(id);
            if (node == null) {
                return false;
            }

            // For both files and folders, use move with same parent to rename
            await this.move(id, node.parent, newName);
            return true;
        } catch {
            return false;
        }
    }

    /**
     * Delete a node and all its children (if it's a folder).
     *
     * @param id - The unique identifier of the node to delete
     * @throws Error if the node doesn't exist
     */
    async deleteNode(id: string): Promise<void> {
        const node = this.nodes.get(id);
        if (node == null) {
            throw new Error(`Node with id ${id} not found`);
        }

        if (node.type === FileType.FOLDER) {
            const folder = node as Folder;
            for (const child of [...folder.children]) {
                await this.deleteNode(child.id);
            }
        }

        if (node.parent != null) {
            const parent = node.parent;
            const index = parent.children.findIndex((child) => child.id === id);
            if (index !== -1) {
                parent.children.splice(index, 1);
            }
            await this.adapter.store(this.convertToStored(parent));
        }

        this.nodes.delete(id);
        await this.adapter.delete(id);

        this.emitEvent({ type: "deleted", node });
    }

    /**
     * Move a node to a different location or rename it in the same location.
     *
     * @param id - The unique identifier of the node to move
     * @param targetParent - The target parent folder (null for root)
     * @param newName - Optional new name for the node (used for renaming)
     * @returns The moved node
     * @throws Error if the node doesn't exist or the move would create a conflict
     */
    async move(id: string, targetParent: Folder | null, newName?: string): Promise<FileSystemNode> {
        const node = this.nodes.get(id);
        if (node == null) {
            throw new Error(`Node with id ${id} not found`);
        }

        const newParent = targetParent ?? this.rootFolder!;
        if (newParent == null || newParent.type !== FileType.FOLDER) {
            throw new Error("Target parent folder not found");
        }

        const actualNewName = newName ?? node.name;
        this.validateName(actualNewName);

        if (newParent.children.some((child) => child.name === actualNewName && child.id !== id)) {
            throw new Error(`File or folder with name "${actualNewName}" already exists in the target folder`);
        }

        const oldPath = node.path;
        const newPath = this.buildPath(newParent.path === "/" ? null : newParent.path, actualNewName);

        if (node.parent != null) {
            const currentParent = node.parent;
            const index = currentParent.children.findIndex((child) => child.id === id);
            if (index !== -1) {
                currentParent.children.splice(index, 1);
            }
            await this.adapter.store(this.convertToStored(currentParent));
        }

        node.name = actualNewName;
        node.parent = newParent;
        node.path = newPath;

        if (node.type === FileType.FOLDER) {
            this.updateDescendantPaths(node as Folder, newPath);
        }

        newParent.children.push(node);

        await this.adapter.store(this.convertToStored(node));
        await this.adapter.store(this.convertToStored(newParent));

        this.emitEvent({ type: "moved", node, oldPath });
        return node;
    }

    /**
     * Update the paths of all descendants when a folder is moved.
     *
     * @param folder - The folder whose descendants need path updates
     * @param newFolderPath - The new path of the folder
     */
    private updateDescendantPaths(folder: Folder, newFolderPath: string): void {
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

    /**
     * Add an event listener for file system changes.
     *
     * @param listener - Function to call when file system events occur
     */
    addEventListener(listener: FileSystemEventListener): void {
        this.eventListeners.push(listener);
    }

    /**
     * Remove an event listener.
     *
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
     *
     * @param event - The event to emit
     */
    private emitEvent(event: FileSystemEvent): void {
        this.eventListeners.forEach((listener) => listener(event));
    }

    /**
     * Build a full path from parent path and node name.
     *
     * @param parentPath - The parent's path (null or "/" for root)
     * @param name - The node's name
     * @returns The full path
     */
    private buildPath(parentPath: string | null, name: string): string {
        if (parentPath == null || parentPath === "/") {
            return `/${name}`;
        }
        return `${parentPath}/${name}`;
    }

    /**
     * Validate a node name for correctness.
     *
     * @param name - The name to validate
     * @throws Error if the name is invalid (empty, contains slashes, etc.)
     */
    private validateName(name: string): void {
        if (name.trim().length === 0) {
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
     *
     * @returns A unique string identifier combining timestamp and random characters
     */
    private generateId(): string {
        return `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
    }
}
