import type { FileSystemAdapter, StoredNode } from "./fileSystemAdapter.js";

/**
 * IndexedDB-based adapter for browser-based persistent file system storage.
 */
export class BrowserFileSystemAdapter implements FileSystemAdapter {
    /**
     * Reference to the IndexedDB database instance
     */
    private db: IDBDatabase | null = null;
    /**
     * Name of the IndexedDB database
     */
    private readonly dbName = "FileSystemDB";
    /**
     * Version of the IndexedDB database
     */
    private readonly dbVersion = 1;
    /**
     * Name of the object store within the database
     */
    private readonly storeName = "nodes";

    /**
     * Initialize the IndexedDB database.
     * Creates the database and object store if they don't exist.
     */
    async initialize(): Promise<void> {
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
    }

    /**
     * Load all nodes from IndexedDB.
     * 
     * @returns Array of all stored nodes
     * @throws Error if the database is not initialized
     */
    async loadAll(): Promise<StoredNode[]> {
        if (!this.db) {
            throw new Error("Database not initialized");
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db!.transaction([this.storeName], "readonly");
            const store = transaction.objectStore(this.storeName);
            const request = store.getAll();

            request.onsuccess = () => {
                resolve(request.result || []);
            };
            request.onerror = () => reject(new Error("Failed to load nodes"));
        });
    }

    /**
     * Store a node in IndexedDB.
     * If a node with the same ID exists, it will be updated.
     * 
     * @param node - The node to store
     * @throws Error if the database is not initialized or the store operation fails
     */
    async store(node: StoredNode): Promise<void> {
        if (!this.db) {
            throw new Error("Database not initialized");
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db!.transaction([this.storeName], "readwrite");
            const store = transaction.objectStore(this.storeName);
            const request = store.put(node);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(new Error("Failed to store node"));
        });
    }

    /**
     * Delete a node from IndexedDB.
     * 
     * @param id - The unique identifier of the node to delete
     * @throws Error if the database is not initialized or the delete operation fails
     */
    async delete(id: string): Promise<void> {
        if (!this.db) {
            throw new Error("Database not initialized");
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db!.transaction([this.storeName], "readwrite");
            const store = transaction.objectStore(this.storeName);
            const request = store.delete(id);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(new Error("Failed to delete node"));
        });
    }
}

