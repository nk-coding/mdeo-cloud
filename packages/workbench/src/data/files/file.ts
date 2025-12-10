/**
 * Enumeration of file system node types.
 */
export enum FileType {
    /**
     * Regular file containing content
     */
    FILE = "file",
    /**
     * Directory/folder that can contain other nodes
     */
    FOLDER = "folder"
}

/**
 * Base metadata shared by all file system nodes.
 */
export interface FileMetadata {
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
}

/**
 * Represents a file in the file system.
 */
export interface File extends FileMetadata {
    /**
     * Always FILE for file nodes
     */
    type: FileType.FILE;
    /**
     * Text content of the file
     */
    content: string;
    /**
     * MIME type of the file content
     */
    mimeType?: string;
}

/**
 * Represents a folder in the file system.
 */
export interface Folder extends FileMetadata {
    /**
     * Always FOLDER for folder nodes
     */
    type: FileType.FOLDER;
    /**
     * Array of child nodes contained in this folder
     */
    children: FileSystemNode[];
}

/**
 * Union type representing any node in the file system.
 */
export type FileSystemNode = File | Folder;

/**
 * Event emitted when a file system operation occurs.
 */
export interface FileSystemEvent {
    /**
     * Type of operation that occurred
     */
    type: "created" | "updated" | "deleted" | "moved";
    /**
     * The node that was affected
     */
    node: FileSystemNode;
    /**
     * Previous path (only for move operations)
     */
    oldPath?: string;
}

/**
 * Function signature for file system event listeners.
 */
export type FileSystemEventListener = (event: FileSystemEvent) => void;

/**
 * Options for creating a new file.
 */
export interface CreateFileOptions {
    /**
     * Name of the file to create
     */
    name: string;
    /**
     * Initial content of the file
     */
    content?: string;
    /**
     * ID of parent folder, null for root
     */
    parentId?: string | null;
}

/**
 * Options for creating a new folder.
 */
export interface CreateFolderOptions {
    /**
     * Name of the folder to create
     */
    name: string;
    /**
     * ID of parent folder, null for root
     */
    parentId?: string | null;
}

/**
 * Options for updating an existing file.
 */
export interface UpdateFileOptions {
    /**
     * New content for the file
     */
    content?: string;
    /**
     * New name for the file
     */
    name?: string;
}

/**
 * Options for moving a node to a different location.
 */
export interface MoveOptions {
    /**
     * ID of the target parent folder
     */
    targetParentId: string | null;
    /**
     * New name for the node (optional)
     */
    newName?: string;
}
