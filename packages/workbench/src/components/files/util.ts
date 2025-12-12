import { FileType, type FileSystemNode } from "@/data/files/file";

/**
 * Sorts an array of FileSystemNode objects such that folders appear before files,
 * and both folders and files are sorted alphabetically by their name.
 *
 * @param nodes Array of FileSystemNode objects to sort
 * @returns An object containing two arrays: one for sorted folders and one for sorted files
 */
export function sortFileSystemNodes(nodes: FileSystemNode[]): { folders: FileSystemNode[]; files: FileSystemNode[] } {
    const folders = nodes.filter((node) => node.type === FileType.FOLDER).sort((a, b) => a.name.localeCompare(b.name));
    const files = nodes.filter((node) => node.type === FileType.FILE).sort((a, b) => a.name.localeCompare(b.name));
    return { folders, files };
}
