import { FileType, type FileSystemNode } from "@/data/files/file";

export function sortFileSystemNodes(nodes: FileSystemNode[]): FileSystemNode[] {
    return nodes.sort((a, b) => {
        // Folders come before files
        if (a.type !== b.type) {
            return a.type === FileType.FOLDER ? -1 : 1;
        }
        // Then sort alphabetically by name
        return a.name.localeCompare(b.name);
    });
}
