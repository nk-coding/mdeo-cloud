<template>
    <ContextMenu>
        <ContextMenuTrigger as-child>
            <TreeItem :data="treeItemData" :items="treeItems" @click="handleClick">
                <template #content>
                    <component :is="entry.type === FileType.FOLDER ? FolderIcon : FileIcon" class="w-4 h-4" />
                    <span>{{ entry.name }}</span>
                </template>
                <template v-if="entry.type === FileType.FOLDER" #item="{ item }">
                    <FileSystemItem
                        :entry="getEntryFromTreeItem(item)"
                        :filesystem="filesystem"
                        @select="$emit('select', $event)"
                        @create-file="$emit('createFile', $event, getEntryFromTreeItem(item).id)"
                        @create-folder="$emit('createFolder', $event, getEntryFromTreeItem(item).id)"
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
import { computed } from "vue";
import { Folder as FolderIcon, File as FileIcon } from "lucide-vue-next";
import TreeItem from "@/components/tree/TreeItem.vue";
import {
    ContextMenu,
    ContextMenuTrigger,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator
} from "@/components/ui/context-menu";
import type { BrowserFileSystem, FileSystemNode } from "@/data/files";
import { FileType } from "@/data/files";

const props = defineProps<{
    entry: FileSystemNode;
    filesystem: BrowserFileSystem;
}>();

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [name: string, parentId?: string];
    createFolder: [name: string, parentId?: string];
    rename: [id: string];
    delete: [id: string];
}>();

const treeItemData = computed(() => ({
    key: props.entry.id
}));

const treeItems = computed(() => {
    // For now, we'll not display nested children directly
    // This would need to be implemented with async loading
    return undefined;
});

const getEntryFromTreeItem = (item: any): FileSystemNode => {
    return item as FileSystemNode;
};

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
</script>
