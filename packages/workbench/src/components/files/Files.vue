<template>
    <div class="files-container h-full w-full">
        <ContextMenu>
            <ContextMenuTrigger as-child>
                <Tree class="h-full w-full p-2" :active-element="activeEntry" :items="rootEntries">
                    <template #item="{ item }">
                        <FileSystemItem
                            :entry="item"
                            :filesystem="filesystem"
                            :fileTypes="fileTypes"
                            @select="handleSelect"
                            @create-file="handleCreateFile"
                            @create-folder="handleCreateFolder"
                            @rename="handleRename"
                            @delete="handleDelete"
                        />
                    </template>
                </Tree>
            </ContextMenuTrigger>
            <ContextMenuContent>
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
import { computed, ref, watch } from "vue";
import Tree from "@/components/tree/Tree.vue";
import FileSystemItem from "./FileSystemItem.vue";
import { ContextMenu, ContextMenuTrigger, ContextMenuContent, ContextMenuItem } from "@/components/ui/context-menu";
import { asyncComputed } from "@vueuse/core";
import type { BrowserFileSystem } from "@/data/files/browserFileSystem";
import { sortFileSystemNodes } from "./util";
import type { FileSystemNode } from "@/data/files/file";
import type { WorkbenchFileType } from "./types";

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

const rootFolder = asyncComputed(async () => {
    return await props.filesystem.getRootFolder();
}, null);

const rootEntries = computed(() => {
    const entries = rootFolder.value ? rootFolder.value.children : [];
    return sortFileSystemNodes(entries);
});

// Watch for filesystem changes and reload

const handleSelect = (entry: FileSystemNode) => {
    activeEntry.value = entry;
    emit("select", entry);
};

const handleCreateFileOfType = async (fileType: WorkbenchFileType, parentId?: string) => {
    const name = prompt(`Enter ${fileType.name.toLowerCase()} name:`);
    if (name) {
        const fullName = name.endsWith(fileType.extension) ? name : `${name}${fileType.extension}`;
        emit("createFile", fullName, parentId, fileType);
    }
};

const handleCreateFile = async (_name: string, parentId?: string) => {
    // Use the first file type as default for backward compatibility
    const firstFileType = props.fileTypes[0];
    if (firstFileType) {
        handleCreateFileOfType(firstFileType, parentId);
    }
};

const handleCreateFolder = async (_name: string, parentId?: string) => {
    const name = prompt("Enter folder name:");
    if (name) {
        emit("createFolder", name, parentId);
    }
};

const handleRename = async (id: string) => {
    try {
        const entry = await props.filesystem.getNode(id);
        if (entry) {
            const newName = prompt("Enter new name:", entry.name);
            if (newName && newName !== entry.name) {
                emit("rename", id, newName);
            }
        }
    } catch (error) {
        console.error("Failed to get entry for rename:", error);
    }
};

const handleDelete = async (id: string) => {
    try {
        const entry = await props.filesystem.getNode(id);
        if (entry && confirm(`Are you sure you want to delete "${entry.name}"?`)) {
            emit("delete", id);
        }
    } catch (error) {
        console.error("Failed to get entry for delete:", error);
    }
};
</script>
