import { FileType, type Uri } from "vscode";
import type { FileSystemNode } from "./file";

/**
 * Traverses the file tree to find a file node by its URI
 *
 * @param root Root of the file tree
 * @param targetUri URI of the target file
 * @returns The found file node or undefined if not found
 */
export function findFileInTree(root: FileSystemNode, targetUri: Uri): FileSystemNode | undefined {
    const path = targetUri.path;
    const segments = path.split("/").filter((s) => s.length > 0);

    if (segments.length <= 1) {
        return root;
    }

    let current: FileSystemNode = root;

    for (let i = 1; i < segments.length; i++) {
        if (current.type !== FileType.Directory) {
            return undefined;
        }

        const child: FileSystemNode | undefined = current.children.find((c) => c.name === segments[i]);
        if (child == undefined) {
            return undefined;
        }

        current = child;
    }

    return current;
}
