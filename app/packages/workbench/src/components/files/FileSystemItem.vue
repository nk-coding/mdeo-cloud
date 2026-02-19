<template>
    <ContextMenu @update:open="handleContextMenuOpen">
        <ContextMenuTrigger as-child>
            <TreeItem
                :data="entry"
                :items="entry.type === FileType.Directory ? entry.children : undefined"
                :mode="isRenaming ? 'edit' : 'default'"
                :is-folder="entry.type === FileType.Directory"
                :has-children="entry.type === FileType.Directory && (entry.children.length > 0 || newItem != undefined)"
                @click="openTab(true, $event)"
                @dblclick="openTab(false, $event)"
                @keydown="handleKeydown"
            >
                <template #content>
                    <FileTypeIcon
                        v-if="entry.type === FileType.File"
                        :model-value="languagePluginByExtension.get(entry.extension)"
                        class="size-4"
                    />
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
            <template v-if="entry.type === FileType.File && fileActions.length > 0">
                <ContextMenuItem
                    v-for="action in contextMenuActions"
                    :key="action.key"
                    @click="() => handleFileAction(action)"
                >
                    <Icon :iconNode="action.icon" :name="action.key" class="size-4 mr-2" />
                    <span>{{ action.name }}</span>
                </ContextMenuItem>
                <ContextMenuSeparator />
            </template>
            <template v-if="entry.type === FileType.Directory">
                <ContextMenuItem
                    v-for="fileType in languagePlugins.filter((plugin) => !plugin.isGenerated)"
                    :key="fileType.id"
                    @click="() => handleCreateFileOfType(fileType)"
                >
                    <FileTypeIcon :model-value="fileType" />
                    <span>Create New {{ fileType.name }}</span>
                </ContextMenuItem>
                <ContextMenuSeparator />
                <ContextMenuItem @click="handleCreateFolder">
                    <FolderIcon />
                    <span>Create New Folder</span>
                </ContextMenuItem>
                <ContextMenuSeparator />
            </template>
            <ContextMenuItem @click="handleRename">
                <EditIcon />
                <span>Rename</span>
            </ContextMenuItem>
            <ContextMenuItem @click="handleDeleteClick">
                <Trash2Icon />
                <span>Delete</span>
            </ContextMenuItem>
        </ContextMenuContent>
    </ContextMenu>

    <AlertDialog v-model:open="showDeleteDialog">
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Delete Folder</AlertDialogTitle>
                <AlertDialogDescription>
                    Are you sure you want to delete the folder "{{ entry.name }}" and all its contents? This action
                    cannot be undone.
                </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction @click="confirmDelete">Delete</AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
</template>

<script setup lang="ts">
import { ref, inject, computed } from "vue";
import { FolderIcon, EditIcon, Trash2Icon, Icon } from "lucide-vue-next";
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
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle
} from "@/components/ui/alert-dialog";
import { type FileSystemNode } from "@/data/filesystem/file";
import type { NewItemState } from "./FileSystemItemList.vue";
import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";
import { workbenchStateKey } from "@/components/workbench/util";
import { treeContextKey } from "../tree/util";
import { FileType } from "@codingame/monaco-vscode-files-service-override";
import { Uri } from "vscode";
import FileTypeIcon from "../FileTypeIcon.vue";
import {
    ActionDisplayLocation,
    createActionProtocol,
    type FileMenuActionData,
    type FileAction
} from "@mdeo/language-common";
import * as vscodeJsonrpc from "vscode-jsonrpc";
import { getFileExtension } from "@/data/filesystem/util";
import plugin from "vue-sonner";

const ActionProtocol = createActionProtocol(vscodeJsonrpc);

const props = defineProps<{
    entry: FileSystemNode;
}>();

const workbenchState = inject(workbenchStateKey)!;
const { monacoApi, languagePlugins, activeTab, languagePluginByExtension, languageClient, pendingAction } =
    workbenchState;
const treeContext = inject(treeContextKey)!;

const emit = defineEmits<{
    select: [entry: FileSystemNode];
    createFile: [uri: Uri, fileType: ResolvedWorkbenchLanguagePlugin];
    createFolder: [uri: Uri];
    rename: [oldUri: Uri, newUri: Uri];
    delete: [uri: Uri];
    move: [itemUri: Uri, targetFolderUri: Uri];
    delegateCreateFile: [fileType: ResolvedWorkbenchLanguagePlugin];
    delegateCreateFolder: [];
}>();

const isRenaming = ref(false);
const showDeleteDialog = ref(false);
const newItem = ref<NewItemState>();
const fileActions = ref<FileAction[]>([]);

const contextMenuActions = computed(() =>
    fileActions.value.filter((action) => action.displayLocations.includes(ActionDisplayLocation.CONTEXT_MENU))
);

async function handleContextMenuOpen(open: boolean): Promise<void> {
    if (open && props.entry.type === FileType.File) {
        await fetchFileActions();
    }
}

async function fetchFileActions(): Promise<void> {
    if (!languageClient.value) {
        fileActions.value = [];
        return;
    }

    if (props.entry.type !== FileType.File) {
        fileActions.value = [];
        return;
    }

    const languagePlugin = languagePluginByExtension.value.get(props.entry.extension);
    if (languagePlugin == undefined) {
        fileActions.value = [];
        return;
    }

    try {
        const response = await languageClient.value.sendRequest(ActionProtocol.GetFileActionsRequest, {
            languageId: languagePlugin.id,
            fileUri: props.entry.uri.toString()
        });
        fileActions.value = response?.actions ?? [];
    } catch {
        fileActions.value = [];
    }
}

function handleFileAction(action: FileAction): void {
    if (props.entry.type !== FileType.File) return;

    const languagePlugin = languagePluginByExtension.value.get(props.entry.extension);
    if (!languagePlugin) return;

    pendingAction.value = {
        type: action.key,
        languageId: languagePlugin.id,
        data: {
            uri: props.entry.uri.toString()
        } satisfies FileMenuActionData
    };
}

async function openTab(temporary: boolean, event?: MouseEvent | KeyboardEvent) {
    if (props.entry.type === FileType.File && !isRenaming.value) {
        const file = props.entry;

        if (event instanceof KeyboardEvent) {
            event.preventDefault();
        }
        await monacoApi.editorService.openEditor({
            resource: file.uri,
            options: {
                preserveFocus: temporary
            }
        });
        if (!temporary && activeTab.value != undefined) {
            activeTab.value.temporary = false;
        }
    }
    emit("select", props.entry);
}

function handleCreateFileOfType(fileType: ResolvedWorkbenchLanguagePlugin) {
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

function handleKeydown(event: KeyboardEvent) {
    if (event.key === "F2") {
        handleRename();
        event.stopPropagation();
    } else if (event.key === "Delete") {
        handleDeleteClick();
        event.stopPropagation();
    } else if (event.key === "Enter") {
        openTab(false, event);
        event.stopPropagation();
    }
}

function handleRename() {
    isRenaming.value = true;
}

function handleDeleteClick() {
    if (isRenaming.value) return;

    if (props.entry.type === FileType.Directory) {
        showDeleteDialog.value = true;
    } else {
        handleDelete();
    }
}

function handleDelete() {
    if (!isRenaming.value) {
        emit("delete", props.entry.uri);
    }
}

function confirmDelete() {
    handleDelete();
    showDeleteDialog.value = false;
}

function handleRenameSubmit(newName: string) {
    if (newName.trim() && newName !== getFileNameWithoutExtension(props.entry.name) && isRenaming.value) {
        const extension = getFileExtension(props.entry.name);
        const fullName = `${newName.trim()}${extension}`;
        const parent = props.entry.parent;
        const newUri = parent ? Uri.joinPath(parent.uri, fullName) : Uri.file(`/${fullName}`);
        emit("rename", props.entry.uri, newUri);
    }
    isRenaming.value = false;
}

function handleCreateFileFromChild(uri: Uri, fileType: ResolvedWorkbenchLanguagePlugin) {
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
            (child) => child.uri.toString() === props.entry.uri.toString() || child.name !== fullName
        ) ?? true
    );
}
</script>
