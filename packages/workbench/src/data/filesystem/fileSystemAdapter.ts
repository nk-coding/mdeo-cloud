/**
 * Internal representation of a node as stored by the adapter.
 */
export interface StoredNode {
    id: string;
    name: string;
    type: "file" | "folder";
    parentId: string | null;
    content?: string;
    fileType?: string;
}

/**
 * Low-level interface for file system storage adapters.
 * Provides minimal operations for persisting and retrieving file system data.
 */
export interface FileSystemAdapter {
    /**
     * Initialize the adapter. Must be called before using other methods.
     */
    initialize(): Promise<void>;

    /**
     * Load all nodes from storage.
     */
    loadAll(): Promise<StoredNode[]>;

    /**
     * Store a node in the underlying storage.
     */
    store(node: StoredNode): Promise<void>;

    /**
     * Delete a node from the underlying storage.
     */
    delete(id: string): Promise<void>;
}
