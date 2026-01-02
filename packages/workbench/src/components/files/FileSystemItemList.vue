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
            :key="item.id.toString()"
            :entry="item"
            @select="$emit('select', $event)"
            @create-file="handleCreateFile"
            @create-folder="handleCreateFolder"
            @rename="handleRename"
            @delete="handleDelete"
            @move="handleMove"
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
            :key="item.id.toString()"
            :entry="item"
            @select="$emit('select', $event)"
            @create-file="handleCreateFile"
            @create-folder="handleCreateFolder"
            @rename="handleRename"
            @delete="handleDelete"
            @move="handleMove"
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
import type { WorkbenchLanguagePlugin } from "@/data/plugin/languagePlugin";
import type { Uri } from "vscode";

export interface NewItemState {
    type: "file" | "folder";
    fileType?: WorkbenchLanguagePlugin;
}

const props = defineProps<{
    parent: Folder;
}>();

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [uri: Uri, fileType: WorkbenchLanguagePlugin];
    createFolder: [uri: Uri];
    rename: [oldUri: Uri, newUri: Uri];
    delete: [uri: Uri];
    move: [itemUri: Uri, targetFolderUri: Uri];
    "update:newItem": [value: NewItemState | null];
}>();

const newItem = defineModel<NewItemState | undefined>("newItem");

const items = computed(() => {
    return sortFileSystemNodes(props.parent.children);
});

function handleCreateFile(uri: Uri, fileType: WorkbenchLanguagePlugin) {
    emit("createFile", uri, fileType);
}

function handleCreateFolder(uri: Uri) {
    emit("createFolder", uri);
}

function handleRename(oldUri: Uri, newUri: Uri) {
    emit("rename", oldUri, newUri);
}

function handleDelete(uri: Uri) {
    emit("delete", uri);
}

function handleMove(itemUri: Uri, targetFolderUri: Uri) {
    emit("move", itemUri, targetFolderUri);
}

function handleNewItemSubmit(uri: Uri, fileType?: WorkbenchLanguagePlugin) {
    if (fileType) {
        emit("createFile", uri, fileType);
    } else {
        emit("createFolder", uri);
    }
    newItem.value = undefined;
}

function handleNewItemCancel() {
    newItem.value = undefined;
}
</script>
