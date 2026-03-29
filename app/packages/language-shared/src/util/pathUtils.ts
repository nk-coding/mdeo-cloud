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
    /** Hierarchical nodes with the common prefix stripped */
    nodes: ActionSchemaFileSelectNode[];
    /**
     * The common path prefix that was removed from all entries (no trailing slash).
     * Prepend this to a node's parent-chain to reconstruct the absolute path.
     */
    rootPath: string;
}

/**
 * Builds a hierarchical file selection tree from a list of absolute file paths.
 * The common ancestor prefix is stripped so the tree shows only the meaningful
 * structure, and the stripped prefix is returned as `rootPath`.
 *
 * @param absolutePaths Array of absolute file paths to include in the tree
 * @returns Tree nodes and the common root path prefix
 */
export function buildFileSelectTree(absolutePaths: string[]): FileSelectTreeResult {
    if (absolutePaths.length === 0) return { nodes: [], rootPath: "" };

    const splitPaths = absolutePaths.map((p) => p.split("/").filter(Boolean));

    // Find the common prefix shared by all paths (stopping one level before the leaf)
    let commonLen = 0;
    const first = splitPaths[0];
    if (first) {
        outer: for (let i = 0; i < first.length - 1; i++) {
            for (const parts of splitPaths) {
                if (parts[i] !== first[i]) break outer;
            }
            commonLen++;
        }
    }

    const rootPath = commonLen > 0 ? "/" + (first ?? []).slice(0, commonLen).join("/") : "";

    interface InternalNode {
        name: string;
        children: InternalNode[];
    }

    const root: InternalNode = { name: "", children: [] };

    for (const absolutePath of absolutePaths) {
        const parts = absolutePath.split("/").filter(Boolean).slice(commonLen);
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

    function toPublic(nodes: InternalNode[]): ActionSchemaFileSelectNode[] {
        return nodes.map((n) => {
            const node: ActionSchemaFileSelectNode = { name: n.name };
            if (n.children.length > 0) node.children = toPublic(n.children);
            return node;
        });
    }

    return { nodes: toPublic(root.children), rootPath };
}
