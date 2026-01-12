/**
 * Represents the result of an API operation that can either succeed or fail.
 */
export type ApiResult<T, E = ApiError> = { success: true; value: T } | { success: false; error: E };

/**
 * Common error codes that can occur across all API operations.
 */
export enum CommonErrorCode {
    Unavailable = "Unavailable",
    Unknown = "Unknown"
}

/**
 * Error codes specific to file system operations.
 */
export enum FileSystemErrorCode {
    FileNotFound = "FileNotFound",
    FileExists = "FileExists",
    FileIsADirectory = "FileIsADirectory",
    FileNotADirectory = "FileNotADirectory",
    DirectoryNotEmpty = "DirectoryNotEmpty"
}

/**
 * Error codes specific to project operations.
 */
export enum ProjectErrorCode {
    ProjectNotFound = "ProjectNotFound"
}

/**
 * Error codes specific to plugin operations.
 */
export enum PluginErrorCode {
    PluginNotFound = "PluginNotFound",
    PluginAlreadyExists = "PluginAlreadyExists",
    PluginAlreadyAddedToProject = "PluginAlreadyAddedToProject",
    PluginNotAddedToProject = "PluginNotAddedToProject"
}

/**
 * Union of all possible error codes.
 */
export type ApiErrorCode = CommonErrorCode | FileSystemErrorCode | ProjectErrorCode | PluginErrorCode;

/**
 * Base interface for API errors.
 */
export interface ApiError<TCode extends ApiErrorCode = ApiErrorCode> {
    /**
     * The error code indicating the type of error.
     */
    code: TCode;
    /**
     * A descriptive error message explaining the failure.
     */
    message: string;
}

/**
 * Error type for common errors (Unavailable, Unknown).
 */
export type CommonError = ApiError<CommonErrorCode>;

/**
 * Error type for file system operations.
 */
export type FileSystemError = ApiError<FileSystemErrorCode | CommonErrorCode>;

/**
 * Error type for project operations.
 */
export type ProjectError = ApiError<ProjectErrorCode | CommonErrorCode>;

/**
 * Error type for plugin operations.
 */
export type PluginError = ApiError<PluginErrorCode | CommonErrorCode>;

/**
 * Namespace containing helper functions for creating ApiResult instances
 */
export namespace ApiResult {
    /**
     * Creates a successful API result.
     * @param value The value to wrap in a successful result.
     * @returns A successful API result containing the provided value.
     */
    export function success<T>(value: T): ApiResult<T, never> {
        return { success: true, value };
    }

    /**
     * Creates a failed API result with a common error.
     * @param code The common error code indicating the type of failure.
     * @param message A descriptive error message explaining the failure.
     * @returns A failed API result containing the common error information.
     */
    export function commonFailure<T>(code: CommonErrorCode, message: string): ApiResult<T, CommonError> {
        return { success: false, error: { code, message } };
    }

    /**
     * Creates a failed API result with a file system error.
     * @param code The file system or common error code indicating the type of failure.
     * @param message A descriptive error message explaining the failure.
     * @returns A failed API result containing the file system error information.
     */
    export function fileSystemFailure<T>(
        code: FileSystemErrorCode | CommonErrorCode,
        message: string
    ): ApiResult<T, FileSystemError> {
        return { success: false, error: { code, message } };
    }

    /**
     * Creates a failed API result with a project error.
     * @param code The project or common error code indicating the type of failure.
     * @param message A descriptive error message explaining the failure.
     * @returns A failed API result containing the project error information.
     */
    export function projectFailure<T>(
        code: ProjectErrorCode | CommonErrorCode,
        message: string
    ): ApiResult<T, ProjectError> {
        return { success: false, error: { code, message } };
    }

    /**
     * Creates a failed API result with a plugin error.
     * @param code The plugin or common error code indicating the type of failure.
     * @param message A descriptive error message explaining the failure.
     * @returns A failed API result containing the plugin error information.
     */
    export function pluginFailure<T>(
        code: PluginErrorCode | CommonErrorCode,
        message: string
    ): ApiResult<T, PluginError> {
        return { success: false, error: { code, message } };
    }
}
