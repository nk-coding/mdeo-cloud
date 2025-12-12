<template>
    <div class="files-container h-full w-full">
        <ContextMenu @update:open="console.log(`update open:`, $event)">
            <ContextMenuTrigger as-child>
                <Tree class="h-full w-full p-2" :active-element="activeEntry">
                    <FileSystemItemList
                        v-if="rootFolder"
                        :parent="rootFolder"
                        :filesystem="filesystem"
                        :fileTypes="fileTypes"
                        v-model:new-item="newItem"
                        @select="handleSelect"
                        @create-file="
                            (name: string, parentId?: string, fileType?: WorkbenchFileType) =>
                                emit('createFile', name, parentId, fileType)
                        "
                        @create-folder="(name: string, parentId?: string) => emit('createFolder', name, parentId)"
                        @rename="(id: string, newName: string) => emit('rename', id, newName)"
                        @delete="(id: string) => emit('delete', id)"
                    />
                </Tree>
            </ContextMenuTrigger>
            <ContextMenuContent @close-auto-focus="$event.preventDefault()">
                <ContextMenuItem
                    v-for="fileType in fileTypes"
                    :key="fileType.extension"
                    @click="() => handleCreateFileOfType(fileType)"
                >
                    <span>Create New {{ fileType.name }}</span>
                </ContextMenuItem>
                <ContextMenuItem @click="handleCreateFolder">
                    <span>Create New Folder</span>
                </ContextMenuItem>
            </ContextMenuContent>
        </ContextMenu>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import Tree from "@/components/tree/Tree.vue";
import { ContextMenu, ContextMenuTrigger, ContextMenuContent, ContextMenuItem } from "@/components/ui/context-menu";
import { asyncComputed } from "@vueuse/core";
import type { BrowserFileSystem } from "@/data/files/browserFileSystem";
import type { FileSystemNode } from "@/data/files/file";
import type { WorkbenchFileType } from "./types";
import FileSystemItemList, { type NewItemState } from "./FileSystemItemList.vue";

const props = defineProps<{
    filesystem: BrowserFileSystem;
    fileTypes: WorkbenchFileType[];
}>();

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [name: string, parentId?: string, fileType?: WorkbenchFileType];
    createFolder: [name: string, parentId?: string];
    rename: [id: string, newName: string];
    delete: [id: string];
}>();

const activeEntry = ref<FileSystemNode | undefined>();

const newItem = ref<NewItemState>();

const rootFolder = asyncComputed(async () => {
    return await props.filesystem.getRootFolder();
}, null);

function handleSelect(entry: FileSystemNode) {
    activeEntry.value = entry;
    emit("select", entry);
}

function handleCreateFileOfType(fileType: WorkbenchFileType) {
    newItem.value = {
        type: "file",
        fileType
    };
}

function handleCreateFolder() {
    newItem.value = {
        type: "folder"
    };
}
</script>
