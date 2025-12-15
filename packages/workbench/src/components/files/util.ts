import { type FileSystemNode } from "@/data/filesystem/file";
import { FileType } from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";

/**
 * Sorts an array of FileSystemNode objects such that folders appear before files,
 * and both folders and files are sorted alphabetically by their name.
 *
 * @param nodes Array of FileSystemNode objects to sort
 * @returns An object containing two arrays: one for sorted folders and one for sorted files
 */
export function sortFileSystemNodes(nodes: FileSystemNode[]): { folders: FileSystemNode[]; files: FileSystemNode[] } {
    const folders = nodes
        .filter((node) => node.type === FileType.Directory)
        .sort((a, b) => a.name.localeCompare(b.name));
    const files = nodes.filter((node) => node.type === FileType.File).sort((a, b) => a.name.localeCompare(b.name));
    return { folders, files };
}
