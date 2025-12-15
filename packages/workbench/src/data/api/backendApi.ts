import type {
    IFileDeleteOptions,
    IFileOverwriteOptions,
    IFileWriteOptions,
    FileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { Project } from "../project/project";

/**
 * Backend API interface for managing projects and file system operations.
 *
 * This interface provides methods for:
 * - File system operations (read, write, delete, rename, mkdir, readdir)
 * - Project management (create, update, delete)
 *
 * All file operations require a projectId to scope the operation to a specific project.
 */
export interface BackendApi {
    /**
     * Reads the contents of a file.
     *
     * @param projectId - The ID of the project containing the file
     * @param path - The path to the file relative to the project root
     * @returns A promise that resolves to the file contents as a Uint8Array
     */
    readFile(projectId: string, path: string): Promise<Uint8Array>;

    /**
     * Writes content to a file.
     *
     * @param projectId - The ID of the project containing the file
     * @param path - The path to the file relative to the project root
     * @param content - The content to write as a Uint8Array
     * @param opts - Write options (e.g., create, overwrite)
     * @returns A promise that resolves when the write operation completes
     */
    writeFile(projectId: string, path: string, content: Uint8Array, opts: IFileWriteOptions): Promise<void>;

    /**
     * Creates a new directory.
     *
     * @param projectId - The ID of the project where the directory should be created
     * @param path - The path to the directory relative to the project root
     * @returns A promise that resolves when the directory is created
     */
    mkdir(projectId: string, path: string): Promise<void>;

    /**
     * Reads the contents of a directory.
     *
     * @param projectId - The ID of the project containing the directory
     * @param path - The path to the directory relative to the project root
     * @returns A promise that resolves to an array of [name, FileType] tuples
     */
    readdir(projectId: string, path: string): Promise<[string, FileType][]>;

    /**
     * Gets the type of a file or directory.
     *
     * @param projectId - The ID of the project containing the file/directory
     * @param path - The path to the file/directory relative to the project root
     * @returns A promise that resolves to the FileType
     */
    stat(projectId: string, path: string): Promise<FileType>;

    /**
     * Deletes a file or directory.
     *
     * @param projectId - The ID of the project containing the file/directory
     * @param path - The path to the file/directory relative to the project root
     * @param opts - Delete options (e.g., recursive, useTrash)
     * @returns A promise that resolves when the delete operation completes
     */
    delete(projectId: string, path: string, opts: IFileDeleteOptions): Promise<void>;

    /**
     * Renames or moves a file or directory.
     *
     * @param projectId - The ID of the project containing the file/directory
     * @param from - The current path relative to the project root
     * @param to - The new path relative to the project root
     * @param opts - Rename options (e.g., overwrite)
     * @returns A promise that resolves when the rename operation completes
     */
    rename(projectId: string, from: string, to: string, opts: IFileOverwriteOptions): Promise<void>;

    /**
     * Gets all projects.
     *
     * @returns A promise that resolves to an array of projects
     */
    getProjects(): Promise<Project[]>;

    /**
     * Creates a new project.
     *
     * @param name - The name of the project
     * @param metadata - Optional metadata for the project
     * @returns A promise that resolves to the ID of the created project
     */
    createProject(name: string): Promise<string>;

    /**
     * Updates an existing project's metadata.
     *
     * @param projectId - The ID of the project to update
     * @param updates - The fields to update (e.g., name, metadata)
     * @returns A promise that resolves when the update completes
     */
    updateProject(projectId: string, updates: { name?: string }): Promise<void>;

    /**
     * Deletes a project and all its associated files.
     *
     * @param projectId - The ID of the project to delete
     * @returns A promise that resolves when the delete operation completes
     */
    deleteProject(projectId: string): Promise<void>;
}
