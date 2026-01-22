import { FileType, type Uri } from "vscode";
import type { FileSystemNode } from "./file";

/**
 * File types based on URI structure
 */
export enum FileCategory {
    /**
     * Regular file within the project
     */
    RegularFile,
    /**
     * Execution summary file
     */
    ExecutionSummary,
    /**
     * Execution result file
     */
    ExecutionResultFile
}

/**
 * Base interface for parsed URI results
 */
export interface BaseParsedUri {
    /**
     * The project ID extracted from the URI
     */
    projectId: string;

    /**
     * The category of the file based on the URI structure
     */
    category: FileCategory;
    /**
     * The execution ID (only present for execution-related files)
     */
    executionId?: string;
}

/**
 * Result of parsing a URI for a regular file.
 */
export interface RegularFileParsedUri extends BaseParsedUri {
    category: FileCategory.RegularFile;
    /**
     * The file path within the project
     */
    path: string;
}

/**
 * Result of parsing a URI for an execution result file.
 */
export interface ExecutionSummaryParsedUri extends BaseParsedUri {
    category: FileCategory.ExecutionSummary;
    /**
     * The execution ID (only present for execution-related files)
     */
    executionId: string;
}

/**
 * Result of parsing a URI for an execution result file.
 */
export interface ExecutionResultFileParsedUri extends BaseParsedUri {
    category: FileCategory.ExecutionResultFile;
    /**
     * The execution ID (only present for execution-related files)
     */
    executionId: string;
    /**
     * The file path within the execution results
     */
    path: string;
}

/**
 * Result of parsing a URI.
 * Contains the extracted project ID, file path, category, and optional execution ID.
 */
export type ParsedUri = RegularFileParsedUri | ExecutionSummaryParsedUri | ExecutionResultFileParsedUri;

/**
 * Parse a URI into projectId, path, and file category.
 * Supports three formats:
 * - schema://projectId/files/path (regular files)
 * - schema://projectId/executions/executionId/summary.md (execution summaries)
 * - schema://projectId/executions/executionId/files/path (execution result files)
 *
 * @param uri The URI to parse
 * @returns Parsed URI information
 */
export function parseUri(uri: Uri): ParsedUri {
    const fullPath = uri.path;
    const parts = fullPath.substring(1).split("/");
    const projectId = parts[0];

    if (projectId == undefined) {
        throw new Error("Invalid URI: missing project ID");
    }

    if (parts[1] === "executions") {
        const executionId = parts[2];
        if (executionId == undefined) {
            throw new Error("Invalid URI: missing execution ID");
        }
        if (parts[3] === "files") {
            const path = "/" + parts.slice(4).join("/");
            return { projectId, path, category: FileCategory.ExecutionResultFile, executionId };
        } else {
            return {
                projectId,
                category: FileCategory.ExecutionSummary,
                executionId
            };
        }
    }

    if (parts[1] === "files") {
        const path = "/" + parts.slice(2).join("/");
        return { projectId, path, category: FileCategory.RegularFile };
    }

    const path = "/" + parts.slice(1).join("/");
    return { projectId, path, category: FileCategory.RegularFile };
}

/**
 * Traverses the file tree to find a file node by its path
 *
 * @param root Root of the file tree
 * @param targetPath Path within the tree (e.g., "/folder/file.txt")
 * @returns The found file node or undefined if not found
 */
export function findFileInTree(root: FileSystemNode, targetPath: string): FileSystemNode | undefined {
    const segments = targetPath.split("/").filter((s) => s.length > 0);

    if (segments.length === 0) {
        return root;
    }

    let current: FileSystemNode = root;

    for (const segment of segments) {
        if (current.type !== FileType.Directory) {
            return undefined;
        }

        const child: FileSystemNode | undefined = current.children.find((c) => c.name === segment);
        if (child == undefined) {
            return undefined;
        }

        current = child;
    }

    return current;
}

/**
 * Gets the file extension from a file path
 *
 * @param filePath The file path
 * @returns The file extension including the dot (e.g., ".txt"), or an empty string if none
 */
export function getFileExtension(filePath: string): string {
    const fileName = filePath.split("/").pop() || "";
    const lastDotIndex = fileName.lastIndexOf(".");
    return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
}
