<template>
    <template v-if="items">
        <NewFileSystemItem
            v-if="newItem?.type === 'folder'"
            :item-type="'folder'"
            :parent="parent"
            @submit="handleNewItemSubmit"
            @cancel="handleNewItemCancel"
        />
        <FileSystemItem
            v-for="item in items.folders"
            :key="item.id"
            :entry="item"
            @select="$emit('select', $event)"
            @create-file="(name, parentId, fileType) => $emit('createFile', name, parentId, fileType)"
            @create-folder="(name, parentId) => $emit('createFolder', name, parentId)"
            @rename="(id, newName) => $emit('rename', id, newName)"
            @delete="$emit('delete', $event)"
            @move="(itemId: string, targetFolderId: string) => $emit('move', itemId, targetFolderId)"
            @delegate-create-file="newItem = { type: 'file', fileType: $event }"
            @delegate-create-folder="newItem = { type: 'folder' }"
        />
        <NewFileSystemItem
            v-if="newItem?.type === 'file'"
            :item-type="'file'"
            :parent="parent"
            :file-type="newItem.fileType"
            @submit="handleNewItemSubmit"
            @cancel="handleNewItemCancel"
        />
        <FileSystemItem
            v-for="item in items.files"
            :key="item.id"
            :entry="item"
            @select="$emit('select', $event)"
            @create-file="(name, parentId, fileType) => $emit('createFile', name, parentId, fileType)"
            @create-folder="(name, parentId) => $emit('createFolder', name, parentId)"
            @rename="(id, newName) => $emit('rename', id, newName)"
            @delete="$emit('delete', $event)"
            @move="(itemId: string, targetFolderId: string) => $emit('move', itemId, targetFolderId)"
            @delegate-create-file="newItem = { type: 'file', fileType: $event }"
            @delegate-create-folder="newItem = { type: 'folder' }"
        />
    </template>
</template>

<script setup lang="ts">
import { computed } from "vue";
import FileSystemItem from "./FileSystemItem.vue";
import NewFileSystemItem from "./NewFileSystemItem.vue";
import type { FileSystemNode, Folder } from "@/data/filesystem/file";
import { sortFileSystemNodes } from "./util";
import type { FileTypePlugin } from "@/data/plugin/fileTypePlugin";

export interface NewItemState {
    type: "file" | "folder";
    fileType?: FileTypePlugin;
}

const props = defineProps<{
    parent: Folder;
}>();

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [name: string, parentId?: string, fileType?: FileTypePlugin];
    createFolder: [name: string, parentId?: string];
    rename: [id: string, newName: string];
    delete: [id: string];
    move: [itemId: string, targetFolderId: string];
    "update:newItem": [value: NewItemState | null];
}>();

const newItem = defineModel<NewItemState | undefined>("newItem");

const items = computed(() => {
    return sortFileSystemNodes(props.parent.children);
});

function handleNewItemSubmit(
    name: string,
    itemType: "file" | "folder",
    parentId?: string,
    fileType?: FileTypePlugin
) {
    if (itemType === "file") {
        emit("createFile", name, parentId, fileType);
    } else {
        emit("createFolder", name, parentId);
    }
    newItem.value = undefined;
}

function handleNewItemCancel() {
    newItem.value = undefined;
}
</script>
