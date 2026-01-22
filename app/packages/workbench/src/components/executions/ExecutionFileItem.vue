<template>
    <ContextMenu @update:open="handleContextMenuOpen">
        <ContextMenuTrigger as-child>
            <TreeItem
                :data="entry"
                :is-folder="isDirectory"
                :has-children="isDirectory"
                mode="default"
                @click="openTab(true, $event)"
                @dblclick="openTab(false, $event)"
                @keydown="handleKeydown"
            >
                <template #content>
                    <FolderIcon v-if="isDirectory" class="size-4 shrink-0" />
                    <FileTypeIcon v-else :model-value="fileTypePlugin" class="size-4 shrink-0" />
                    <span class="truncate">{{ entry.name }}</span>
                </template>
                <template v-if="isDirectory" #items>
                    <ExecutionFileItem
                        v-for="child in sortedChildren"
                        :key="child.uri.toString()"
                        :entry="child"
                        :execution-id="executionId"
                        @select="$emit('select', $event)"
                    />
                </template>
            </TreeItem>
        </ContextMenuTrigger>
        <ContextMenuContent
            v-if="!isDirectory && contextMenuActions.length > 0"
            @close-auto-focus="$event.preventDefault()"
        >
            <ContextMenuItem
                v-for="action in contextMenuActions"
                :key="action.key"
                @click="() => handleFileAction(action)"
            >
                <Icon :iconNode="action.icon" :name="action.key" class="size-4 mr-2" />
                <span>{{ action.name }}</span>
            </ContextMenuItem>
        </ContextMenuContent>
    </ContextMenu>
</template>

<script setup lang="ts">
import { computed, inject, ref } from "vue";
import { FolderIcon, Icon } from "lucide-vue-next";
import TreeItem from "@/components/tree/TreeItem.vue";
import FileTypeIcon from "@/components/FileTypeIcon.vue";
import { ContextMenu, ContextMenuTrigger, ContextMenuContent, ContextMenuItem } from "@/components/ui/context-menu";
import type { FileSystemNode, File, Folder } from "@/data/filesystem/file";
import { FileType } from "vscode";
import { workbenchStateKey } from "@/components/workbench/util";
import {
    ActionDisplayLocation,
    createActionProtocol,
    type FileMenuActionData,
    type FileAction
} from "@mdeo/language-common";
import * as vscodeJsonrpc from "vscode-jsonrpc";

const ActionProtocol = createActionProtocol(vscodeJsonrpc);

const props = defineProps<{
    entry: FileSystemNode;
    executionId: string;
}>();

const emit = defineEmits<{
    select: [entry: FileSystemNode];
}>();

const workbenchState = inject(workbenchStateKey)!;
const { languagePluginByExtension, languageClient, pendingAction, monacoApi, activeTab } = workbenchState;

const fileActions = ref<FileAction[]>([]);

const contextMenuActions = computed(() =>
    fileActions.value.filter((action) => action.displayLocations.includes(ActionDisplayLocation.CONTEXT_MENU))
);

const itemId = computed(() => props.entry.uri.toString());

const isDirectory = computed(() => props.entry.type === FileType.Directory);

const fileExtension = computed(() => {
    if (isDirectory.value) {
        return "";
    }
    return (props.entry as File).extension;
});

const fileTypePlugin = computed(() => {
    return languagePluginByExtension.value.get(fileExtension.value);
});

const sortedChildren = computed(() => {
    if (isDirectory.value == undefined) {
        return [];
    }

    const folder = props.entry as Folder;

    return [...folder.children].sort((a, b) => {
        const aIsDir = a.type === FileType.Directory;
        const bIsDir = b.type === FileType.Directory;

        if (aIsDir !== bIsDir) {
            return aIsDir ? -1 : 1;
        }
        return a.name.localeCompare(b.name);
    });
});

async function handleContextMenuOpen(open: boolean): Promise<void> {
    if (open && !isDirectory.value) {
        await fetchFileActions();
    }
}

async function fetchFileActions(): Promise<void> {
    if (!languageClient.value) {
        fileActions.value = [];
        return;
    }

    const languagePlugin = languagePluginByExtension.value.get(fileExtension.value);
    if (!languagePlugin) {
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
    const languagePlugin = languagePluginByExtension.value.get(fileExtension.value);
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
    if (props.entry.type === FileType.File) {
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

function handleKeydown(event: KeyboardEvent) {
    if (event.key === "Enter") {
        openTab(false, event);
    }
}
</script>
