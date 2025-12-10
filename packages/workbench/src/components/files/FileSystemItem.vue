<template>
    <ContextMenu v-model:open="isContextMenuOpen">
        <ContextMenuTrigger as-child>
            <TreeItem
                :data="entry"
                :items="treeItems"
                @click="handleClick"
                :data-state="isContextMenuOpenFixed ? 'open' : 'closed'"
            >
                <template #content>
                    <component :is="entry.type === FileType.FOLDER ? FolderIcon : FileIcon" class="w-4 h-4" />
                    <span>{{ entry.name }}</span>
                </template>
                <template v-if="entry.type === FileType.FOLDER" #item="{ item }">
                    <FileSystemItem
                        :entry="item"
                        :filesystem="filesystem"
                        :fileTypes="fileTypes"
                        @select="$emit('select', $event)"
                        @create-file="(name, parentId) => $emit('createFile', name, parentId)"
                        @create-folder="(name, parentId) => $emit('createFolder', name, parentId)"
                        @rename="$emit('rename', $event)"
                        @delete="$emit('delete', $event)"
                    />
                </template>
            </TreeItem>
        </ContextMenuTrigger>
        <ContextMenuContent>
            <ContextMenuItem @click="handleCreateFile">
                <span>Create New Metamodel File</span>
            </ContextMenuItem>
            <ContextMenuItem @click="handleCreateFolder">
                <span>Create New Folder</span>
            </ContextMenuItem>
            <ContextMenuSeparator />
            <ContextMenuItem @click="handleRename">
                <span>Rename</span>
            </ContextMenuItem>
            <ContextMenuItem @click="handleDelete" class="text-red-600">
                <span>Delete</span>
            </ContextMenuItem>
        </ContextMenuContent>
    </ContextMenu>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { Folder as FolderIcon, File as FileIcon } from "lucide-vue-next";
import TreeItem from "@/components/tree/TreeItem.vue";
import {
    ContextMenu,
    ContextMenuTrigger,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator
} from "@/components/ui/context-menu";
import { FileType, type FileSystemNode } from "@/data/files/file";
import type { BrowserFileSystem } from "@/data/files/browserFileSystem";
import type { WorkbenchFileType } from "./types";
import { sortFileSystemNodes } from "./util";

const props = defineProps<{
    entry: FileSystemNode;
    filesystem: BrowserFileSystem;
    fileTypes: WorkbenchFileType[];
}>();

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [name: string, parentId?: string];
    createFolder: [name: string, parentId?: string];
    rename: [id: string];
    delete: [id: string];
}>();

const treeItems = computed(() => {
    if (props.entry.type === FileType.FOLDER) {
        return sortFileSystemNodes(props.entry.children);
    }
    return undefined;
});

const handleClick = () => {
    emit("select", props.entry);
};

const handleCreateFile = () => {
    const parentId = props.entry.type === FileType.FOLDER ? props.entry.id : props.entry.parentId;
    emit("createFile", "", parentId || undefined);
};

const handleCreateFolder = () => {
    const parentId = props.entry.type === FileType.FOLDER ? props.entry.id : props.entry.parentId;
    emit("createFolder", "", parentId || undefined);
};

const handleRename = () => {
    emit("rename", props.entry.id);
};

const handleDelete = () => {
    emit("delete", props.entry.id);
};

const isContextMenuOpen = ref(false);
const isContextMenuOpenFixed = ref(false);

watch(isContextMenuOpen, (newVal) => {
    if (newVal) {
        isContextMenuOpenFixed.value = true;
    } else {
        setTimeout(() => {
            isContextMenuOpenFixed.value = false;
        }, 200);
    }
});
</script>
