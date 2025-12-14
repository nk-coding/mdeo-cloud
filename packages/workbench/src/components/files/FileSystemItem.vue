<template>
    <ContextMenu>
        <ContextMenuTrigger as-child>
            <TreeItem
                :data="entry"
                :items="entry.type === FileType.FOLDER ? entry.children : undefined"
                :mode="isRenaming ? 'edit' : 'default'"
                :is-folder="entry.type === FileType.FOLDER"
                :has-children="entry.type === FileType.FOLDER && (entry.children.length > 0 || newItem != undefined)"
                @click="openTab(true, $event)"
                @dblclick="openTab(false, $event)"
                @keydown.enter="openTab(false, $event)"
            >
                <template #content>
                    <FileIcon v-if="entry.type === FileType.FILE" :is="FolderIcon" class="w-4 h-4" />
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
                <template v-if="entry.type === FileType.FOLDER" #items>
                    <FileSystemItemList
                        :parent="entry"
                        v-model:new-item="newItem"
                        @select="$emit('select', $event)"
                        @create-file="(name, parentId, fileType) => $emit('createFile', name, parentId, fileType)"
                        @create-folder="(name, parentId) => $emit('createFolder', name, parentId)"
                        @rename="(id, newName) => $emit('rename', id, newName)"
                        @delete="$emit('delete', $event)"
                        @move="(itemId: string, targetFolderId: string) => $emit('move', itemId, targetFolderId)"
                    />
                </template>
            </TreeItem>
        </ContextMenuTrigger>
        <ContextMenuContent @close-auto-focus="$event.preventDefault()">
            <ContextMenuItem
                v-for="fileType in workbenchState.supportedFileTypes.value"
                :key="fileType.extension"
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
import { FileType, type FileSystemNode } from "@/data/filesystem/file";
import type { NewItemState } from "./FileSystemItemList.vue";
import type { FileTypePlugin } from "@/data/plugin/fileTypePlugin";
import { workbenchStateKey } from "@/data/workbenchState";
import { expandedItemsKey } from "../tree/util";

const props = defineProps<{
    entry: FileSystemNode;
}>();

const workbenchState = inject(workbenchStateKey)!;
const expandedItems = inject(expandedItemsKey)!;

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [name: string, parentId: string | undefined, fileType: FileTypePlugin];
    createFolder: [name: string, parentId?: string];
    rename: [id: string, newName: string];
    delete: [id: string];
    move: [itemId: string, targetFolderId: string];
    delegateCreateFile: [fileType: FileTypePlugin];
    delegateCreateFolder: [];
}>();

const isRenaming = ref(false);

const newItem = ref<NewItemState>();

function openTab(temporary: boolean, event?: MouseEvent | KeyboardEvent) {
    if (props.entry.type === FileType.FILE && !isRenaming.value) {
        const file = props.entry;

        if (event instanceof KeyboardEvent) {
            event.preventDefault();
        }

        const existingTab = workbenchState.value.tabs.value.find((tab) => tab.file.path === file.path);

        if (existingTab) {
            workbenchState.value.activeTab.value = existingTab;
            if (!temporary && existingTab.temporary) {
                existingTab.temporary = false;
            }
        } else {
            const newTab = {
                file: file,
                temporary: temporary
            };
            workbenchState.value.tabs.value.push(newTab);
            workbenchState.value.activeTab.value = newTab;
        }
        emit("select", props.entry);
    }
}

function handleCreateFileOfType(fileType: FileTypePlugin) {
    if (props.entry.type === FileType.FOLDER) {
        newItem.value = {
            type: "file",
            fileType
        };
        expandedItems.value.add(props.entry);
    } else {
        emit("delegateCreateFile", fileType);
    }
}

function handleCreateFolder() {
    if (props.entry.type === FileType.FOLDER) {
        newItem.value = {
            type: "folder"
        };
        expandedItems.value.add(props.entry);
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
        const fullName = extension ? `${newName.trim()}${extension}` : newName.trim();
        emit("rename", props.entry.id, fullName);
    }
    isRenaming.value = false;
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
    return parent?.children.every((child) => child.id === props.entry.id || child.name !== fullName) ?? true;
}
</script>
