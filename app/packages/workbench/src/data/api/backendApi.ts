import type {
    IFileDeleteOptions,
    IFileOverwriteOptions,
    IFileWriteOptions,
    FileType
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { Project } from "../project/project";
import type { ApiResult, FileSystemError, ProjectError, PluginError, CommonError } from "./apiResult";
import type { BackendPlugin, ResolvedPlugin } from "./pluginTypes";

/**
 * User information returned from login
 */
export interface User {
    id: string;
    username: string;
    isAdmin: boolean;
}

/**
 * User info for owner listings
 */
export interface UserInfo {
    id: string;
    username: string;
}

/**
 * Backend API interface for managing projects and file system operations.
 *
 * This interface provides methods for:
 * File system operations (read, write, delete, rename, mkdir, readdir)
 * Project management (create, update, delete)
 * Authentication (login, register, logout)
 * User management (get current user, change password)
 * Project ownership (add/remove owners)
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

    /**
     * Creates a new plugin.
     *
     * @param url The URL where the plugin is hosted
     * @returns A promise that resolves to an ApiResult containing the ID of the created plugin
     *          Possible errors: PluginAlreadyExists, Unavailable, Unknown
     */
    createPlugin(url: string): Promise<ApiResult<string, PluginError>>;

    /**
     * Deletes a plugin.
     *
     * @param pluginId The ID of the plugin to delete
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: PluginNotFound, Unavailable, Unknown
     */
    deletePlugin(pluginId: string): Promise<ApiResult<void, PluginError>>;

    /**
     * Gets all plugins.
     *
     * @returns A promise that resolves to an ApiResult containing an array of plugins
     *          Possible errors: Unavailable, Unknown
     */
    getPlugins(): Promise<ApiResult<BackendPlugin[], PluginError>>;

    /**
     * Gets all plugins associated with a project.
     *
     * @param projectId The ID of the project
     * @returns A promise that resolves to an ApiResult containing an array of plugins
     *          Possible errors: ProjectNotFound, Unavailable, Unknown
     */
    getProjectPlugins(projectId: string): Promise<ApiResult<BackendPlugin[], PluginError>>;

    /**
     * Adds a plugin to a project.
     *
     * @param projectId The ID of the project
     * @param pluginId The ID of the plugin to add
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: ProjectNotFound, PluginNotFound, PluginAlreadyAddedToProject, Unavailable, Unknown
     */
    addPluginToProject(projectId: string, pluginId: string): Promise<ApiResult<void, PluginError>>;

    /**
     * Removes a plugin from a project.
     *
     * @param projectId The ID of the project
     * @param pluginId The ID of the plugin to remove
     * @returns A promise that resolves to an ApiResult indicating success or failure
     *          Possible errors: ProjectNotFound, PluginNotFound, PluginNotAddedToProject, Unavailable, Unknown
     */
    removePluginFromProject(projectId: string, pluginId: string): Promise<ApiResult<void, PluginError>>;

    /**
     * Resolves a plugin to its implementation.
     * This will return a more detailed interface in the future.
     *
     * @param pluginId The ID of the plugin to resolve
     * @returns A promise that resolves to an ApiResult containing the resolved plugin
     *          Possible errors: PluginNotFound, Unavailable, Unknown
     */
    resolvePlugin(pluginId: string): Promise<ApiResult<ResolvedPlugin, PluginError>>;

    /**
     * Get the current authenticated user
     */
    getCurrentUser(): Promise<ApiResult<User, CommonError>>;

    /**
     * Login with username and password
     */
    login(username: string, password: string): Promise<ApiResult<User, CommonError>>;

    /**
     * Register a new user account
     */
    register(username: string, password: string): Promise<ApiResult<User, CommonError>>;
    
    /**
     * Logout the current user
     */
    logout(): Promise<void>;
    
    /**
     * Change the current user's password
     */
    changePassword(currentPassword: string, newPassword: string): Promise<ApiResult<void, CommonError>>;
    
    /**
     * Get project owners
     */
    getProjectOwners(projectId: string): Promise<ApiResult<UserInfo[], ProjectError>>;
    
    /**
     * Add an owner to a project
     */
    addProjectOwner(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>>;
    
    /**
     * Remove an owner from a project
     */
    removeProjectOwner(projectId: string, userId: string): Promise<ApiResult<void, ProjectError>>;

    /**
     * Precache file tree for a project to optimize subsequent file operations.
     * When called with a different project, the previous cache is dropped.
     * 
     * @param project The project to precache
     */
    precache(project: Project): Promise<void>;
}
