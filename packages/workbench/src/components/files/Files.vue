<template>
    <div class="files-container h-full w-full m-1">
        <ContextMenu>
            <ContextMenuTrigger as-child>
                <Tree
                    class="h-full w-full p-1 -m-1"
                    :active-element="activeEntry"
                    :enable-drag-and-drop="true"
                    :drag-and-drop-callbacks="dragAndDropCallbacks"
                    :expanded-items="expandedItems"
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
import { ref, inject, computed, watch } from "vue";
import Tree from "@/components/tree/Tree.vue";
import { ContextMenu, ContextMenuTrigger, ContextMenuContent, ContextMenuItem } from "@/components/ui/context-menu";
import { asyncComputed } from "@vueuse/core";
import type { FileSystemNode, Folder } from "@/data/filesystem/file";
import { FileType } from "@/data/filesystem/file";
import type { DragAndDropCallbacks } from "@/components/tree/util";
import FileSystemItemList, { type NewItemState } from "./FileSystemItemList.vue";
import type { FileTypePlugin } from "@/data/plugin/fileTypePlugin";
import { workbenchStateKey } from "@/data/workbenchState";

const workbenchState = inject(workbenchStateKey)!;

const activeEntry = ref<FileSystemNode | undefined>();
const expandedItems = ref<Set<FileSystemNode>>(new Set());

const newItem = ref<NewItemState>();

const rootFolder = asyncComputed(async () => {
    return await workbenchState.value.fileSystem.getRootFolder();
}, null);

const activeTab = computed(() => workbenchState.value.activeTab.value);

watch(
    activeTab,
    (newTab) => {
        if (newTab != undefined) {
            const file = newTab.file;
            activeEntry.value = file;
            let parent = file.parent;
            while (parent != undefined) {
                expandedItems.value.add(parent);
                parent = parent.parent;
            }
        }
    },
    { immediate: true }
);

function handleSelect(entry: FileSystemNode) {
    activeEntry.value = entry;
}

async function handleCreateFile(name: string, parentId: string | undefined, fileType: FileTypePlugin) {
    if (name.trim()) {
        const fileName =
            fileType?.extension && !name.endsWith(fileType.extension) ? `${name}${fileType.extension}` : name;

        await workbenchState.value.fileSystem.createFile({
            name: fileName,
            parentId,
            plugin: fileType
        });
    }
}

async function handleCreateFolder(name: string, parentId?: string) {
    if (name.trim()) {
        await workbenchState.value.fileSystem.createFolderSimple(name, parentId);
    }
}

async function handleRename(id: string, newName: string) {
    await workbenchState.value.fileSystem.renameEntry(id, newName);
}

async function handleDelete(id: string) {
    await workbenchState.value.fileSystem.deleteNode(id);
}

async function handleMove(itemId: string, targetFolderId: string) {
    const targetFolder = await workbenchState.value.fileSystem.getNode(targetFolderId);
    await workbenchState.value.fileSystem.moveNode(itemId, { targetParent: targetFolder as Folder });
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

            if (draggedNode!.parent?.id === rootFolder.value.id) {
                return;
            }

            handleMove(draggedItem.id, rootFolder.value.id);
        }
    }
};
</script>
