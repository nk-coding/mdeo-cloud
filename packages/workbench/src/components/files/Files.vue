<template>
    <ScrollArea class="files-container flex-1 min-h-0 w-full">
        <ContextMenu>
            <ContextMenuTrigger as-child>
                <Tree
                    ref="treeRef"
                    class="flex-1 w-full p-2"
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
                    v-for="fileType in fileTypePlugins"
                    :key="fileType.id"
                    @click="() => handleCreateFileOfType(fileType)"
                >
                    <span>Create New {{ fileType.name }}</span>
                </ContextMenuItem>
                <ContextMenuItem @click="handleCreateFolderFromContext">
                    <span>Create New Folder</span>
                </ContextMenuItem>
            </ContextMenuContent>
        </ContextMenu>
    </ScrollArea>
</template>

<script setup lang="ts">
import { ref, inject, watch, nextTick, useTemplateRef } from "vue";
import Tree from "@/components/tree/Tree.vue";
import { ContextMenu, ContextMenuTrigger, ContextMenuContent, ContextMenuItem } from "@/components/ui/context-menu";
import type { FileSystemNode } from "@/data/filesystem/file";
import type { DragAndDropCallbacks } from "@/components/tree/util";
import FileSystemItemList, { type NewItemState } from "./FileSystemItemList.vue";
import type { LanguagePlugin } from "@/data/plugin/languagePlugin";
import ScrollArea from "../ui/scroll-area/ScrollArea.vue";
import { VSBuffer } from "@codingame/monaco-vscode-api/vscode/vs/base/common/buffer";
import { workbenchStateKey } from "../workbench/util";
import { Uri } from "vscode";
import { FileType } from "@codingame/monaco-vscode-files-service-override";
import { findFileInTree } from "@/data/filesystem/util";

const workbenchState = inject(workbenchStateKey)!;
const { fileTree: rootFolder, activeTab, monacoApi, languagePlugins: fileTypePlugins, tabs } = workbenchState;

const activeEntry = ref<FileSystemNode | undefined>();
const expandedItems = ref<Set<FileSystemNode>>(new Set());

const newItem = ref<NewItemState>();

const treeRef = useTemplateRef("treeRef");

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
            nextTick(() => {
                const element = treeRef.value!.$el.querySelector(`[data-active=true]`) as HTMLElement;
                if (element != undefined) {
                    const rect = element.getBoundingClientRect();
                    const completelyVisible =
                        rect.top >= 0 && rect.bottom <= (window.innerHeight || document.documentElement.clientHeight);
                    if (!completelyVisible) {
                        element.scrollIntoView({ block: "center", behavior: "instant" });
                    }
                }
            });
        } else {
            activeEntry.value = undefined;
        }
    },
    { immediate: true }
);

function handleSelect(entry: FileSystemNode) {
    activeEntry.value = entry;
}

async function handleCreateFile(uri: Uri, fileType: LanguagePlugin) {
    const fileService = monacoApi.fileService;
    await fileService.createFile(uri, VSBuffer.fromString(fileType.defaultContent ?? ""));
    await nextTick();

    const file = findFileInTree(rootFolder, uri);

    if (file != undefined && file.type === FileType.File) {
        const existingTab = tabs.value.find((tab) => tab.file.id.toString() === file.id.toString());

        if (existingTab) {
            workbenchState.activeTab.value = existingTab;
            existingTab.temporary = false;
        } else {
            const newTab = {
                file: file,
                temporary: false
            };
            tabs.value.push(newTab);
            workbenchState.activeTab.value = newTab;
        }
    }
}

async function handleCreateFolder(uri: Uri) {
    const fileService = monacoApi.fileService;
    await fileService.createFolder(uri);
}

async function handleRename(oldUri: Uri, newUri: Uri) {
    const fileService = monacoApi.fileService;
    await fileService.move(oldUri, newUri, false);
}

async function handleDelete(uri: Uri) {
    const fileService = monacoApi.fileService;
    await fileService.del(uri, { recursive: true });
}

async function handleMove(itemUri: Uri, targetFolderUri: Uri) {
    const fileService = monacoApi.fileService;
    const fileName = itemUri.path.split("/").pop() || "";
    const newUri = Uri.joinPath(targetFolderUri, fileName);
    await fileService.move(itemUri, newUri, false);
}

function handleCreateFileOfType(fileType: LanguagePlugin) {
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
        const draggedNode = draggedItem as unknown as FileSystemNode;
        const targetNode = targetItem as unknown as FileSystemNode;

        if (targetNode.type !== FileType.Directory) {
            return false;
        }

        if (draggedNode.id.toString() === targetNode.id.toString()) {
            return false;
        }

        return true;
    },

    onDrop: async (draggedItem, targetItem) => {
        const draggedNode = draggedItem as unknown as FileSystemNode;
        const targetNode = targetItem as unknown as FileSystemNode;

        if (targetNode.type !== FileType.Directory) {
            return;
        }

        if (draggedNode.type === FileType.Directory) {
            let current = targetNode.parent;
            while (current) {
                if (current.id.toString() === draggedNode.id.toString()) {
                    return;
                }
                current = current.parent;
            }
        }

        handleMove(draggedNode.id, targetNode.id);
    },

    onTreeDrop: async (draggedItem) => {
        const draggedNode = draggedItem as unknown as FileSystemNode;

        if (draggedNode.parent?.id.toString() === rootFolder.id.toString()) {
            return;
        }

        handleMove(draggedNode.id, rootFolder.id);
    }
};
</script>
<style scoped>
.files-container :deep(div[data-reka-scroll-area-viewport] > div:first-child) {
    @apply min-h-full flex flex-col;
}
</style>
