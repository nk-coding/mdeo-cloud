/**
 * Utility functions for path manipulation.
 */

import type { ActionSchemaFileSelectNode } from "@mdeo/language-common";

/**
 * Calculates the relative path from one file to another.
 *
 * @param fromPath Absolute path of the source file
 * @param toPath Absolute path of the target file
 * @returns Relative path from source file to target file
 */
export function calculateRelativePath(fromPath: string, toPath: string): string {
    const fromParts = fromPath.split("/");
    const toParts = toPath.split("/");

    fromParts.pop();

    let commonLength = 0;
    const maxLength = Math.min(fromParts.length, toParts.length);
    while (commonLength < maxLength && fromParts[commonLength] === toParts[commonLength]) {
        commonLength++;
    }

    const upLevels = fromParts.length - commonLength;
    const relativeParts: string[] = [];

    if (upLevels > 0) {
        for (let i = 0; i < upLevels; i++) {
            relativeParts.push("..");
        }
    } else {
        relativeParts.push(".");
    }

    relativeParts.push(...toParts.slice(commonLength));

    return relativeParts.join("/");
}

/**
 * Result of building a file selection tree.
 */
export interface FileSelectTreeResult {
    /**
     * Hierarchical nodes with the common prefix stripped
     */
    nodes: ActionSchemaFileSelectNode[];
    /**
     * The common path prefix that was removed from all entries (no trailing slash).
     * Prepend this to a node's parent-chain to reconstruct the absolute path.
     */
    rootPath: string;
}

/**
 * Builds a hierarchical tree of file selection nodes from a list of absolute file paths.
 */
interface InternalNode {
    name: string;
    children: InternalNode[];
}

/**
 * Converts internal tree nodes to public-facing {@link ActionSchemaFileSelectNode} format.
 *
 * @param nodes List of internal nodes to convert
 * @returns List of public-facing file select nodes
 */
function toPublic(nodes: InternalNode[]): ActionSchemaFileSelectNode[] {
    return nodes.map((n) => {
        const node: ActionSchemaFileSelectNode = { name: n.name };
        if (n.children.length > 0) node.children = toPublic(n.children);
        return node;
    });
}

/**
 * Builds a file selection tree from a list of absolute paths.
 *
 * Strips the common `<projectId>/files` prefix from all paths, using the first
 * path to determine the prefix. All paths are assumed to share the same prefix.
 *
 * @param absolutePaths List of absolute file paths to build the tree from.
 * @returns A tree of {@link ActionSchemaFileSelectNode} nodes and the stripped root path.
 */
export function buildFileSelectTree(absolutePaths: string[]): FileSelectTreeResult {
    if (absolutePaths.length === 0) return { nodes: [], rootPath: "" };

    const firstParts = absolutePaths[0].split("/").filter(Boolean);
    const markerIndex = firstParts.indexOf("files");
    const prefixLen = markerIndex >= 0 ? markerIndex + 1 : 0;
    const rootPath = "/" + firstParts.slice(0, prefixLen).join("/");

    const root: InternalNode = { name: "", children: [] };

    for (const absolutePath of absolutePaths) {
        const parts = absolutePath.split("/").filter(Boolean).slice(prefixLen);
        let current = root;
        for (const name of parts) {
            let child = current.children.find((c) => c.name === name);
            if (!child) {
                child = { name, children: [] };
                current.children.push(child);
            }
            current = child;
        }
    }

    return { nodes: toPublic(root.children), rootPath };
}
