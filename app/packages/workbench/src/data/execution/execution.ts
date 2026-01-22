import { FileType, Uri } from "vscode";
import type { FileSystemNode, Folder } from "../filesystem/file";
import { markRaw } from "vue";

/**
 * Represents the state of an execution.
 */
export type ExecutionState = "submitted" | "initializing" | "running" | "completed" | "cancelled" | "failed";

/**
 * Represents a file entry in an execution's result tree (from backend/plugin - flat structure).
 */
export interface ExecutionFileEntry {
    /**
     * Name of the file or directory (can be full path like "results/summary.m")
     */
    name: string;
    /**
     * Type of the entry (1 = file, 2 = directory)
     */
    type: number;
}

/**
 * File type constants matching backend FileType.
 */
export const ExecutionFileType = {
    FILE: 1,
    DIRECTORY: 2
} as const;

/**
 * Represents an execution.
 */
export interface Execution {
    /**
     * Unique identifier for the execution
     */
    id: string;
    /**
     * ID of the project this execution belongs to
     */
    projectId: string;
    /**
     * Path to the source file at the time of execution
     */
    filePath: string;
    /**
     * ID of the language plugin handling this execution
     */
    languageId: string;
    /**
     * Display name of the execution
     */
    name: string;
    /**
     * Current state of the execution
     */
    state: ExecutionState;
    /**
     * Optional progress indication text
     */
    progressText: string | null;
    /**
     * ISO 8601 timestamp when the execution was created/submitted
     */
    createdAt: string;
    /**
     * ISO 8601 timestamp when the execution started running, or null if not yet started
     */
    startedAt: string | null;
    /**
     * ISO 8601 timestamp when the execution finished, or null if not yet finished
     */
    finishedAt: string | null;
}

/**
 * Represents an execution with its result file tree.
 */
export interface ExecutionWithTree {
    /**
     * The execution metadata
     */
    execution: Execution;
    /**
     * Optional file tree for execution results (only available when completed)
     */
    fileTree: ExecutionFileEntry[] | null;
}

/**
 * Request payload for creating a new execution.
 */
export interface CreateExecutionRequest {
    /**
     * Path to the file to execute
     */
    filePath: string;
    /**
     * Arbitrary JSON data for the execution (e.g., function to execute)
     */
    data: unknown;
}

/**
 * Checks if an execution is in a terminal state (completed, cancelled, or failed).
 *
 * @param state The execution state to check
 * @returns True if the execution is in a terminal state
 */
export function isTerminalState(state: ExecutionState): boolean {
    return state === "completed" || state === "cancelled" || state === "failed";
}

/**
 * Checks if an execution can be cancelled.
 *
 * @param state The execution state to check
 * @returns True if the execution can be cancelled
 */
export function canCancelExecution(state: ExecutionState): boolean {
    return !isTerminalState(state);
}

/**
 * Converts a flat list of execution file entries into a tree structure.
 *
 * @param entries Flat list of file entries from backend/plugin
 * @param executionId Execution ID for creating unique node IDs
 * @param projectId Project ID for creating unique node URIs
 * @returns Root folder containing the tree structure
 */
export function buildExecutionFileTree(entries: ExecutionFileEntry[], executionId: string, projectId: string): Folder {
    const id = `/${projectId}/executions/${executionId}/files`;
    const root: Folder = {
        id,
        uri: markRaw(Uri.file(id)),
        name: "",
        type: FileType.Directory,
        parent: null,
        children: []
    };

    const sortedEntries = [...entries].sort((a, b) => {
        const aDepth = a.name.split("/").length;
        const bDepth = b.name.split("/").length;
        return aDepth - bDepth;
    });

    const folderMap = new Map<string, Folder>();
    folderMap.set("", root);

    for (const entry of sortedEntries) {
        const path = entry.name;
        const segments = path.split("/").filter((s) => s.length > 0);
        const name = segments[segments.length - 1] || "";
        const parentPath = segments.slice(0, -1).join("/");

        let parent = folderMap.get(parentPath);
        if (!parent) {
            parent = root;
        }

        const existingChild = parent.children.find((c) => c.name === name);
        if (existingChild) {
            continue;
        }

        if (entry.type === ExecutionFileType.DIRECTORY) {
            const id = `/${projectId}/executions/${executionId}/files/${path}`;
            const folder: Folder = {
                id,
                uri: markRaw(Uri.file(id)),
                name,
                type: FileType.Directory,
                parent,
                children: []
            };
            parent.children.push(folder);
            folderMap.set(path, folder);
        } else {
            const lastDotIndex = name.lastIndexOf(".");
            const extension = lastDotIndex > 0 ? name.substring(lastDotIndex) : "";
            const id = `/${projectId}/executions/${executionId}/files/${path}`;
            const file: FileSystemNode = {
                id,
                uri: markRaw(Uri.file(id)),
                name,
                type: FileType.File,
                parent,
                extension
            };
            parent.children.push(file);
        }
    }

    return root;
}
