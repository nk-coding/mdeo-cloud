import type { Uri } from "vscode";
import { FileType } from "@codingame/monaco-vscode-files-service-override";

/**
 * Base metadata shared by all file system nodes.
 */
export interface FileMetadata {
    /**
     * Display name of the node
     */
    name: string;
    /**
     * Type of the node (file or folder)
     */
    type: FileType;
    /**
     * Full path from root to this node
     */
    id: Uri;
    /**
     * Parent folder of this node, or null if this is the root
     */
    parent: Folder | null;
}

/**
 * Represents a file in the file system.
 */
export interface File extends FileMetadata {
    /**
     * Always FILE for file nodes
     */
    type: FileType.File;
}

/**
 * Represents a folder in the file system.
 */
export interface Folder extends FileMetadata {
    /**
     * Always FOLDER for folder nodes
     */
    type: FileType.Directory;
    /**
     * Array of child nodes contained in this folder
     */
    children: FileSystemNode[];
}

/**
 * Union type representing any node in the file system.
 */
export type FileSystemNode = File | Folder;
