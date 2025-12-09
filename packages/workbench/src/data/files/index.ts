/**
 * @fileoverview Hierarchical file system API with IndexedDB backend for browser environments.
 *
 * This module provides a complete file system abstraction with:
 * - Hierarchical folder structure
 * - Async operations for all file/folder manipulations
 * - Event system for change notifications
 * - IndexedDB persistence in the browser
 * - Extensible architecture for additional backends
 */

/**
 * Core types and interfaces
 */
export type {
    FileMetadata,
    File,
    Folder,
    FileSystemNode,
    FileSystemEvent,
    FileSystemEventListener,
    CreateFileOptions,
    CreateFolderOptions,
    UpdateFileOptions,
    MoveOptions
} from "./file.js";

export { FileType } from "./file.js";

/**
 * Abstract file system base class and utilities
 */
export { FileSystem, type FileSystemStats } from "./fileSystem.js";

/**
 * IndexedDB-based browser implementation
 */
export { BrowserFileSystem } from "./browserFileSystem.js";
