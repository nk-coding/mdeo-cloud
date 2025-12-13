<template>
    <div class="files-container h-full w-full">
        <ContextMenu>
            <ContextMenuTrigger as-child>
                <Tree
                    class="h-full w-full p-2 -m-2"
                    :active-element="activeEntry"
                    :enable-drag-and-drop="true"
                    :drag-and-drop-callbacks="dragAndDropCallbacks"
                >
                    <FileSystemItemList
                        v-if="rootFolder"
                        :parent="rootFolder"
                        v-model:new-item="newItem"
                        @select="handleSelect"
                        @create-file="handleCreateFile"
                        @create-folder="handleCreateFolder"
                        @rename="handleRename"
                        @delete="handleDelete"
                    />
                </Tree>
            </ContextMenuTrigger>
            <ContextMenuContent @close-auto-focus="$event.preventDefault()">
                <ContextMenuItem
                    v-for="fileType in workbenchState.supportedFileTypes.value"
                    :key="fileType.extension"
                    @click="() => handleCreateFileOfType(fileType)"
                >
                    <span>Create New {{ fileType.name }}</span>
                </ContextMenuItem>
                <ContextMenuItem @click="handleCreateFolderFromContext">
                    <span>Create New Folder</span>
                </ContextMenuItem>
            </ContextMenuContent>
        </ContextMenu>
    </div>
</template>

<script setup lang="ts">
import { ref, inject } from "vue";
import Tree from "@/components/tree/Tree.vue";
import { ContextMenu, ContextMenuTrigger, ContextMenuContent, ContextMenuItem } from "@/components/ui/context-menu";
import { asyncComputed } from "@vueuse/core";
import type { FileSystemNode } from "@/data/filesystem/file";
import { FileType } from "@/data/filesystem/file";
import type { DragAndDropCallbacks } from "@/components/tree/util";
import FileSystemItemList, { type NewItemState } from "./FileSystemItemList.vue";
import type { FileTypePlugin } from "@/data/plugin/fileTypePlugin";
import { workbenchStateKey } from "@/data/workbenchState";

const workbenchState = inject(workbenchStateKey)!;

const activeEntry = ref<FileSystemNode | undefined>();

const newItem = ref<NewItemState>();

const rootFolder = asyncComputed(async () => {
    return await workbenchState.value.fileSystem.getRootFolder();
}, null);

function handleSelect(entry: FileSystemNode) {
    activeEntry.value = entry;
    console.log("Selected:", entry);
}

async function handleCreateFile(name: string, parentId?: string, fileType?: FileTypePlugin) {
    if (name.trim()) {
        try {
            const content = fileType?.defaultContent || "";
            const fileName =
                fileType?.extension && !name.endsWith(fileType.extension) ? `${name}${fileType.extension}` : name;

            const newFile = await workbenchState.value.fileSystem.createFile({
                name: fileName,
                content,
                parentId
            });
            console.log("Created file:", newFile);
        } catch (error) {
            console.error("Failed to create file:", error);
        }
    }
}

async function handleCreateFolder(name: string, parentId?: string) {
    if (name.trim()) {
        try {
            const newFolder = await workbenchState.value.fileSystem.createFolderSimple(name, parentId);
            console.log("Created folder:", newFolder);
        } catch (error) {
            console.error("Failed to create folder:", error);
        }
    }
}

async function handleRename(id: string, newName: string) {
    try {
        const node = await workbenchState.value.fileSystem.getNode(id);
        if (!node) {
            console.error("Node not found");
            return;
        }

        await workbenchState.value.fileSystem.renameEntry(id, newName);
        console.log("Renamed: success");
    } catch (error) {
        console.error("Failed to rename:", error);
    }
}

async function handleDelete(id: string) {
    try {
        await workbenchState.value.fileSystem.deleteNode(id);
        console.log("Deleted: success");
    } catch (error) {
        console.error("Failed to delete:", error);
    }
}

async function handleMove(itemId: string, targetFolderId: string) {
    try {
        const targetFolder = await workbenchState.value.fileSystem.getNode(targetFolderId);
        console.log("Moving item", itemId, "to folder", targetFolder);
        if (targetFolder && targetFolder.type === FileType.FOLDER) {
            await workbenchState.value.fileSystem.moveNode(itemId, { targetParent: targetFolder });
            console.log("Moved: success");
        } else {
            console.error("Target is not a folder");
        }
    } catch (error) {
        console.error("Failed to move:", error);
    }
}

function handleCreateFileOfType(fileType: FileTypePlugin) {
    newItem.value = {
        type: "file",
        fileType
    };
}

function handleCreateFolderFromContext() {
    newItem.value = {
        type: "folder"
    };
}

const dragAndDropCallbacks: DragAndDropCallbacks = {
    canDrop: (draggedItem, targetItem) => {
        const targetNode = targetItem as FileSystemNode;

        if (targetNode.type !== FileType.FOLDER) {
            return false;
        }

        if (draggedItem.id === targetNode.id) {
            return false;
        }

        return true;
    },

    onDrop: async (draggedItem, targetItem) => {
        const targetNode = targetItem as FileSystemNode;

        if (targetNode.type !== FileType.FOLDER) {
            return;
        }

        const draggedNode = await workbenchState.value.fileSystem.getNode(draggedItem.id);
        if (!draggedNode) {
            return;
        }

        if (draggedNode.type === FileType.FOLDER) {
            let current = targetNode.parent;
            while (current) {
                if (current.id === draggedNode.id) {
                    return;
                }
                current = current.parent;
            }
        }

        handleMove(draggedItem.id, targetNode.id);
    },

    onTreeDrop: async (draggedItem) => {
        if (rootFolder.value) {
            const draggedNode = await workbenchState.value.fileSystem.getNode(draggedItem.id);
            if (!draggedNode) {
                console.error("Dragged item not found");
                return;
            }

            if (draggedNode.parent?.id === rootFolder.value.id) {
                return;
            }

            handleMove(draggedItem.id, rootFolder.value.id);
        }
    }
};
</script>
