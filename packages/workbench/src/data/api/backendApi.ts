import type {
    IFileDeleteOptions,
    IFileOverwriteOptions,
    IFileWriteOptions,
    FileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { Project } from "../project/project";
import type { ApiResult, FileSystemError, ProjectError } from "./apiResult";

/**
 * Backend API interface for managing projects and file system operations.
 *
 * This interface provides methods for:
 * File system operations (read, write, delete, rename, mkdir, readdir)
 * Project management (create, update, delete)
 *
 * All file operations require a projectId to scope the operation to a specific project.
 * All operations return ApiResult to handle errors explicitly without throwing exceptions.
 */
export interface BackendApi {
    /**
     * Reads the contents of a file.
     *
     * @param projectId The ID of the project containing the file
     * @param path The path to the file relative to the project root
     * @returns A promise that resolves to an ApiResult containing the file contents as a Uint8Array
     *          Possible errors: FileNotFound, FileIsADirectory, Unavailable, Unknown
     */
    readFile(projectId: string, path: string): Promise<ApiResult<Uint8Array, FileSystemError>>;

    /**
     * Writes content to a file.
     *
     * @param projectId The ID of the project containing the file
     * @param path The path to the file relative to the project root
     * @param content The content to write as a Uint8Array
     * @param opts Write options (e.g., create, overwrite)
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: FileIsADirectory, FileExists, FileNotFound, Unavailable, Unknown
     */
    writeFile(
        projectId: string,
        path: string,
        content: Uint8Array,
        opts: IFileWriteOptions
    ): Promise<ApiResult<void, FileSystemError>>;

    /**
     * Creates a new directory.
     *
     * @param projectId The ID of the project where the directory should be created
     * @param path The path to the directory relative to the project root
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: FileExists, Unavailable, Unknown
     */
    mkdir(projectId: string, path: string): Promise<ApiResult<void, FileSystemError>>;

    /**
     * Reads the contents of a directory.
     *
     * @param projectId The ID of the project containing the directory
     * @param path The path to the directory relative to the project root
     * @returns A promise that resolves to an ApiResult containing an array of [name, FileType] tuples
     *          Possible errors: FileNotFound, FileNotADirectory, Unavailable, Unknown
     */
    readdir(projectId: string, path: string): Promise<ApiResult<[string, FileType][], FileSystemError>>;

    /**
     * Gets the type of a file or directory.
     *
     * @param projectId The ID of the project containing the file/directory
     * @param path The path to the file/directory relative to the project root
     * @returns A promise that resolves to an ApiResult containing the FileType
     *          Possible errors: FileNotFound, Unavailable, Unknown
     */
    stat(projectId: string, path: string): Promise<ApiResult<FileType, FileSystemError>>;

    /**
     * Deletes a file or directory.
     *
     * @param projectId The ID of the project containing the file/directory
     * @param path The path to the file/directory relative to the project root
     * @param opts Delete options (e.g., recursive, useTrash)
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: FileNotFound, DirectoryNotEmpty, Unavailable, Unknown
     */
    delete(projectId: string, path: string, opts: IFileDeleteOptions): Promise<ApiResult<void, FileSystemError>>;

    /**
     * Renames or moves a file or directory.
     *
     * @param projectId The ID of the project containing the file/directory
     * @param from The current path relative to the project root
     * @param to The new path relative to the project root
     * @param opts Rename options (e.g., overwrite)
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: FileNotFound, FileExists, Unavailable, Unknown
     */
    rename(
        projectId: string,
        from: string,
        to: string,
        opts: IFileOverwriteOptions
    ): Promise<ApiResult<void, FileSystemError>>;

    /**
     * Gets all projects.
     *
     * @returns A promise that resolves to an ApiResult containing an array of projects
     *          Possible errors: Unavailable, Unknown
     */
    getProjects(): Promise<ApiResult<Project[], ProjectError>>;

    /**
     * Creates a new project.
     *
     * @param name The name of the project
     * @param metadata Optional metadata for the project
     * @returns A promise that resolves to an ApiResult containing the ID of the created project
     *          Possible errors: Unavailable, Unknown
     */
    createProject(name: string): Promise<ApiResult<string, ProjectError>>;

    /**
     * Updates an existing project's metadata.
     *
     * @param projectId The ID of the project to update
     * @param updates The fields to update (e.g., name, metadata)
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: ProjectNotFound, Unavailable, Unknown
     */
    updateProject(projectId: string, updates: { name?: string }): Promise<ApiResult<void, ProjectError>>;

    /**
     * Deletes a project and all its associated files.
     *
     * @param projectId The ID of the project to delete
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: Unavailable, Unknown
     */
    deleteProject(projectId: string): Promise<ApiResult<void, ProjectError>>;

    /**
     * Reads metadata for a file.
     *
     * @param projectId The ID of the project containing the file
     * @param path The path to the file relative to the project root
     * @returns A promise that resolves to an ApiResult containing the metadata object
     *          If no metadata exists, returns an empty object
     *          Possible errors: FileNotFound, Unavailable, Unknown
     */
    readMetadata(projectId: string, path: string): Promise<ApiResult<object, FileSystemError>>;

    /**
     * Writes metadata for a file.
     *
     * @param projectId The ID of the project containing the file
     * @param path The path to the file relative to the project root
     * @param metadata The metadata object to write
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: FileNotFound, Unavailable, Unknown
     */
    writeMetadata(projectId: string, path: string, metadata: object): Promise<ApiResult<void, FileSystemError>>;
}
