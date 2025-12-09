<template>
    <div class="files-container h-full w-full">
        <ContextMenu>
            <ContextMenuTrigger as-child>
                <Tree class="h-full w-full p-2" :active-element="activeEntry">
                    <ul class="flex flex-col gap-1">
                        <FileSystemItem
                            v-for="entry in rootEntries"
                            :key="entry.id"
                            :entry="entry"
                            :filesystem="filesystem"
                            @select="handleSelect"
                            @create-file="handleCreateFile"
                            @create-folder="handleCreateFolder"
                            @rename="handleRename"
                            @delete="handleDelete"
                        />
                    </ul>
                </Tree>
            </ContextMenuTrigger>
            <ContextMenuContent>
                <ContextMenuItem @click="handleCreateFile()">
                    <span>Create New Metamodel File</span>
                </ContextMenuItem>
                <ContextMenuItem @click="handleCreateFolder()">
                    <span>Create New Folder</span>
                </ContextMenuItem>
            </ContextMenuContent>
        </ContextMenu>
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import Tree from "@/components/tree/Tree.vue";
import FileSystemItem from "./FileSystemItem.vue";
import { ContextMenu, ContextMenuTrigger, ContextMenuContent, ContextMenuItem } from "@/components/ui/context-menu";
import type { BrowserFileSystem, FileSystemNode } from "@/data/files";

const props = defineProps<{
    filesystem: BrowserFileSystem;
}>();

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [name: string, parentId?: string];
    createFolder: [name: string, parentId?: string];
    rename: [id: string, newName: string];
    delete: [id: string];
}>();

const activeEntry = ref<FileSystemNode | undefined>();
const rootEntries = ref<FileSystemNode[]>([]);

// Load root entries when component mounts
const loadRootEntries = async () => {
    try {
        rootEntries.value = await props.filesystem.getRootEntries();
    } catch (error) {
        console.error("Failed to load root entries:", error);
        rootEntries.value = [];
    }
};

// Watch for filesystem changes and reload
watch(() => props.filesystem, loadRootEntries, { immediate: true });

const handleSelect = (entry: FileSystemNode) => {
    activeEntry.value = entry;
    emit("select", entry);
};

const handleCreateFile = async (parentId?: string) => {
    const name = prompt("Enter file name:");
    if (name) {
        const fullName = name.endsWith(".metamodel") ? name : `${name}.metamodel`;
        emit("createFile", fullName, parentId);
        // Refresh the root entries after creation
        await loadRootEntries();
    }
};

const handleCreateFolder = async (parentId?: string) => {
    const name = prompt("Enter folder name:");
    if (name) {
        emit("createFolder", name, parentId);
        // Refresh the root entries after creation
        await loadRootEntries();
    }
};

const handleRename = async (id: string) => {
    try {
        const entry = await props.filesystem.getEntry(id);
        if (entry) {
            const newName = prompt("Enter new name:", entry.name);
            if (newName && newName !== entry.name) {
                emit("rename", id, newName);
                // Refresh the root entries after rename
                await loadRootEntries();
            }
        }
    } catch (error) {
        console.error("Failed to get entry for rename:", error);
    }
};

const handleDelete = async (id: string) => {
    try {
        const entry = await props.filesystem.getEntry(id);
        if (entry && confirm(`Are you sure you want to delete "${entry.name}"?`)) {
            emit("delete", id);
            // Refresh the root entries after deletion
            await loadRootEntries();
        }
    } catch (error) {
        console.error("Failed to get entry for delete:", error);
    }
};
</script>
