import type { LangiumInstance } from "../langium/langiumInstance.js";
import type { ServerApi } from "../service/serverApi.js";

/**
 * Execution lifecycle states
 */
export enum ExecutionState {
    /**
     * Execution has been submitted but not yet started
     */
    SUBMITTED = "submitted",
    /**
     * Execution is being initialized
     */
    INITIALIZING = "initializing",
    /**
     * Execution is currently running
     */
    RUNNING = "running",
    /**
     * Execution completed successfully
     */
    COMPLETED = "completed",
    /**
     * Execution was cancelled
     */
    CANCELLED = "cancelled",
    /**
     * Execution failed with an error
     */
    FAILED = "failed"
}

/**
 * Context provided to execution handlers.
 * Contains the minimal data needed to setup a langium instance and execute.
 */
export interface ExecutionContext {
    /**
     * Unique identifier for the execution
     */
    executionId: string;

    /**
     * Project identifier
     */
    project: string;

    /**
     * Path to the source file being executed
     */
    filePath: string;

    /**
     * Content of the source file
     */
    fileContent: string;

    /**
     * Version of the source file
     */
    fileVersion: number;

    /**
     * Arbitrary data for the execution request
     */
    data: object;

    /**
     * JWT token for authentication
     */
    jwt: string;

    /**
     * Contribution plugins for the language
     */
    contributionPlugins: object[];

    /**
     * The configured Langium instance for this execution request.
     */
    instance: LangiumInstance<any>;

    /**
     * Server API configured with the current request context.
     */
    serverApi: ServerApi;
}

/**
 * Small JSON metadata object stored on execution records.
 */
export type ExecutionMetadata = Record<string, unknown>;

/**
 * Context for execution follow-up operations (summary/files/cancel/delete).
 */
export interface ExecutionRequestContext {
    /**
     * Unique identifier for the execution.
     */
    executionId: string;
    /**
     * Project identifier from the authenticated JWT context.
     */
    project: string;
    /**
     * JWT token for authentication.
     */
    jwt: string;
    /**
     * Optional execution metadata attached by backend.
     */
    metadata?: ExecutionMetadata;
    /**
     * The configured Langium instance for this request.
     */
    instance: LangiumInstance<any>;
    /**
     * Server API configured with the current request context.
     */
    serverApi: ServerApi;
}

/**
 * Result of an execution handler's canHandle check
 */
export interface CanHandleResult {
    /**
     * Whether this handler can process the execution request
     */
    canHandle: boolean;

    /**
     * Optional reason for not being able to handle (for debugging)
     */
    reason?: string;
}

/**
 * Response from the execute method.
 */
export interface ExecuteResponse {
    /**
     * Display name for the execution
     */
    name: string;
}

/**
 * Entry in a file tree.
 */
export interface FileEntry {
    /**
     * Path to the file or directory
     */
    path: string;

    /**
     * Whether this is a directory
     */
    isDirectory: boolean;

    /**
     * File size in bytes (only for files)
     */
    size?: number;
}

/**
 * Handler for execution requests.
 *
 * Execution handlers are responsible for:
 * - Determining if they can handle a specific execution request
 * - Executing the request
 * - Providing access to execution results
 * - Managing execution lifecycle (cancel, delete)
 *
 * @template T The return type of the execute method
 */
export interface ExecutionHandler<T = unknown> {
    /**
     * Determines if this handler can process the given execution request.
     * This method analyzes the execution data to decide if the handler
     * is appropriate for this specific execution.
     *
     * @param context The execution context with minimal data
     * @returns Promise resolving to a result indicating if the handler can process this request
     */
    canHandle(context: ExecutionContext): Promise<CanHandleResult>;

    /**
     * Executes the requested operation.
     * This method is only called if canHandle returns true.
     *
     * @param context The execution context with minimal data
     * @returns Promise resolving to the execution response
     */
    execute(context: ExecutionContext): Promise<T>;

    /**
     * Gets a markdown summary of the execution results.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise resolving to a markdown-formatted summary
     */
    getSummary(context: ExecutionRequestContext): Promise<string>;

    /**
     * Gets the file tree of execution results.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise resolving to the list of files and directories
     */
    getFileTree(context: ExecutionRequestContext): Promise<FileEntry[]>;
    /**
     * Reads a specific file from the execution results.
     *
     * @param executionId Unique identifier for the execution
     * @param path Path to the file to read
     * @param jwt JWT token for authentication
     * @returns Promise resolving to the file contents
     */
    getFile(context: ExecutionRequestContext, path: string): Promise<Buffer>;
    /**
     * Cancels a running execution.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise that resolves when the execution is cancelled
     */
    cancel(context: ExecutionRequestContext): Promise<void>;

    /**
     * Deletes an execution and its results.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise that resolves when the execution is deleted
     */
    delete(context: ExecutionRequestContext): Promise<void>;
}

/**
 * Execution lifecycle event types
 */
export enum ExecutionEventType {
    /**
     * Execution state has changed
     */
    STATE_CHANGED = "stateChanged",
    /**
     * Execution progress has been updated
     */
    PROGRESS_UPDATED = "progressUpdated",
    /**
     * Execution has completed
     */
    COMPLETED = "completed",
    /**
     * Execution has failed
     */
    FAILED = "failed",
    /**
     * Execution was cancelled
     */
    CANCELLED = "cancelled"
}

/**
 * Execution lifecycle event data
 */
export interface ExecutionEvent {
    /**
     * Type of the event
     */
    type: ExecutionEventType;

    /**
     * Execution ID this event relates to
     */
    executionId: string;

    /**
     * New state if this is a state change event
     */
    state?: ExecutionState;

    /**
     * Progress text if this is a progress update
     */
    progressText?: string;

    /**
     * Error message if this is a failure event
     */
    error?: string;

    /**
     * ISO 8601 timestamp of the event
     */
    timestamp: string;
}

/**
 * Callback for execution lifecycle events
 *
 * @param event The execution event
 */
export type ExecutionEventCallback = (event: ExecutionEvent) => void | Promise<void>;
