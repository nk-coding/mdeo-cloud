<template>
    <ContextMenu>
        <ContextMenuTrigger as-child>
            <TreeItem
                :data="entry"
                :items="entry.type === FileType.Directory ? entry.children : undefined"
                :mode="isRenaming ? 'edit' : 'default'"
                :is-folder="entry.type === FileType.Directory"
                :has-children="entry.type === FileType.Directory && (entry.children.length > 0 || newItem != undefined)"
                @click="openTab(true, $event)"
                @dblclick="openTab(false, $event)"
                @keydown.enter="openTab(false, $event)"
                @keydown.f2="handleRename"
                @keydown.delete="handleDelete"
            >
                <template #content>
                    <FileIcon v-if="entry.type === FileType.File" :is="FolderIcon" class="w-4 h-4" />
                    <span v-if="isRenaming" class="flex flex-1">
                        <TreeItemInput
                            :model-value="getFileNameWithoutExtension(entry.name)"
                            :validate="validateRename"
                            @submit="handleRenameSubmit"
                            @cancel="handleRenameCancel"
                        />{{ getFileExtension(entry.name) }}
                    </span>
                    <span v-else>{{ entry.name }}</span>
                </template>
                <template v-if="entry.type === FileType.Directory" #items>
                    <FileSystemItemList
                        :parent="entry"
                        v-model:new-item="newItem"
                        @select="$emit('select', $event)"
                        @create-file="handleCreateFileFromChild"
                        @create-folder="handleCreateFolderFromChild"
                        @rename="handleRenameFromChild"
                        @delete="handleDeleteFromChild"
                        @move="handleMoveFromChild"
                    />
                </template>
            </TreeItem>
        </ContextMenuTrigger>
        <ContextMenuContent @close-auto-focus="$event.preventDefault()">
            <ContextMenuItem
                v-for="fileType in workbenchState.languagePlugins.value"
                :key="fileType.id"
                @click="() => handleCreateFileOfType(fileType)"
            >
                <span>Create New {{ fileType.name }}</span>
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
import { ref, inject } from "vue";
import { Folder as FolderIcon, File as FileIcon } from "lucide-vue-next";
import TreeItem from "@/components/tree/TreeItem.vue";
import TreeItemInput from "../tree/TreeItemInput.vue";
import FileSystemItemList from "./FileSystemItemList.vue";
import {
    ContextMenu,
    ContextMenuTrigger,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator
} from "@/components/ui/context-menu";
import { type FileSystemNode } from "@/data/filesystem/file";
import type { NewItemState } from "./FileSystemItemList.vue";
import type { LanguagePlugin } from "@/data/plugin/languagePlugin";
import { workbenchStateKey } from "@/components/workbench/util";
import { treeContextKey } from "../tree/util";
import { FileType } from "@codingame/monaco-vscode-files-service-override";
import { Uri } from "vscode";

const props = defineProps<{
    entry: FileSystemNode;
}>();

const workbenchState = inject(workbenchStateKey)!;
const treeContext = inject(treeContextKey)!;

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [uri: Uri, fileType: LanguagePlugin];
    createFolder: [uri: Uri];
    rename: [oldUri: Uri, newUri: Uri];
    delete: [uri: Uri];
    move: [itemUri: Uri, targetFolderUri: Uri];
    delegateCreateFile: [fileType: LanguagePlugin];
    delegateCreateFolder: [];
}>();

const isRenaming = ref(false);

const newItem = ref<NewItemState>();

function openTab(temporary: boolean, event?: MouseEvent | KeyboardEvent) {
    if (props.entry.type === FileType.File && !isRenaming.value) {
        const file = props.entry;

        if (event instanceof KeyboardEvent) {
            event.preventDefault();
        }

        workbenchState.openTab(file, temporary);
    }
    emit("select", props.entry);
}

function handleCreateFileOfType(fileType: LanguagePlugin) {
    if (props.entry.type === FileType.Directory) {
        newItem.value = {
            type: "file",
            fileType
        };
        treeContext.expandedItems.value.add(props.entry);
    } else {
        emit("delegateCreateFile", fileType);
    }
}

function handleCreateFolder() {
    if (props.entry.type === FileType.Directory) {
        newItem.value = {
            type: "folder"
        };
        treeContext.expandedItems.value.add(props.entry);
    } else {
        emit("delegateCreateFolder");
    }
}

function handleRename() {
    isRenaming.value = true;
}

function handleDelete() {
    emit("delete", props.entry.id);
}

function handleRenameSubmit(newName: string) {
    if (newName.trim() && newName !== getFileNameWithoutExtension(props.entry.name)) {
        const extension = getFileExtension(props.entry.name);
        const fullName = `${newName.trim()}${extension}`;
        const parent = props.entry.parent;
        const newUri = parent ? Uri.joinPath(parent.id, fullName) : Uri.file(`/${fullName}`);
        emit("rename", props.entry.id, newUri);
    }
    isRenaming.value = false;
}

function handleCreateFileFromChild(uri: Uri, fileType: LanguagePlugin) {
    emit("createFile", uri, fileType);
}

function handleCreateFolderFromChild(uri: Uri) {
    emit("createFolder", uri);
}

function handleRenameFromChild(oldUri: Uri, newUri: Uri) {
    emit("rename", oldUri, newUri);
}

function handleDeleteFromChild(uri: Uri) {
    emit("delete", uri);
}

function handleMoveFromChild(itemUri: Uri, targetFolderUri: Uri) {
    emit("move", itemUri, targetFolderUri);
}

function handleRenameCancel() {
    isRenaming.value = false;
}

function getFileNameWithoutExtension(filename: string): string {
    const lastDotIndex = filename.lastIndexOf(".");
    return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
}

function getFileExtension(filename: string): string {
    const lastDotIndex = filename.lastIndexOf(".");
    return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
}

function validateRename(newName: string): boolean {
    if (newName.trim().length === 0) {
        return false;
    }
    const fullName = newName.trim() + getFileExtension(props.entry.name);
    if (fullName === props.entry.name) {
        return true;
    }
    const parent = props.entry.parent;
    return (
        parent?.children.every(
            (child) => child.id.toString() === props.entry.id.toString() || child.name !== fullName
        ) ?? true
    );
}
</script>
