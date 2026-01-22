<template>
    <div class="flex flex-col h-full">
        <SidebarPanelHeader label="Executions">
            <template #actions>
                <Tooltip v-if="executionsList.length > 0">
                    <TooltipTrigger asChild>
                        <Button variant="ghost" size="icon" class="h-8 w-8" @click="handleDeleteAll">
                            <Trash2 class="size-4" />
                        </Button>
                    </TooltipTrigger>
                    <TooltipContent side="right">Delete All</TooltipContent>
                </Tooltip>
            </template>
        </SidebarPanelHeader>
        <ScrollArea class="executions-container flex-1 min-h-0 w-full">
            <Tree
                ref="treeRef"
                class="flex-1 w-full p-2"
                :active-element="activeEntry"
                :enable-drag-and-drop="false"
                :expanded-items="expandedItems"
            >
                <template v-if="executionsList.length > 0">
                    <ExecutionItem
                        v-for="executionData in executionsList"
                        :key="executionData.execution.id"
                        :execution-data="executionData"
                        @select="handleSelect"
                        @cancel="handleCancel"
                        @delete="handleDelete"
                    />
                </template>
                <div v-else class="text-sm text-muted-foreground text-center py-4">No executions yet</div>
            </Tree>
        </ScrollArea>

        <AlertDialog v-model:open="showDeleteDialog">
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete Execution</AlertDialogTitle>
                    <AlertDialogDescription>
                        Are you sure you want to delete the execution "{{ executionToDelete?.execution.name }}"? This
                        action cannot be undone.
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction @click="confirmDelete">Delete</AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>

        <AlertDialog v-model:open="showDeleteAllDialog">
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete All Executions</AlertDialogTitle>
                    <AlertDialogDescription>
                        Are you sure you want to delete all {{ executionsList.length }} execution(s)? This action cannot be
                        undone.
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction @click="confirmDeleteAll">Delete All</AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    </div>
</template>

<script setup lang="ts">
import { ref, inject, computed, onActivated, watch, nextTick, useTemplateRef } from "vue";
import Tree from "@/components/tree/Tree.vue";
import SidebarPanelHeader from "@/components/sidebar/SidebarPanelHeader.vue";
import ScrollArea from "@/components/ui/scroll-area/ScrollArea.vue";
import { Tooltip, TooltipTrigger, TooltipContent } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
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
import { workbenchStateKey } from "@/components/workbench/util";
import ExecutionItem from "./ExecutionItem.vue";
import { Trash2 } from "lucide-vue-next";
import type { ExecutionWithLoadedTree } from "@/data/workbenchState";
import type { FileSystemNode } from "@/data/filesystem/file";
import { Uri } from "vscode";
import { parseUri, FileCategory, findFileInTree } from "@/data/filesystem/util";
import { showApiError } from "@/lib/notifications";

const workbenchState = inject(workbenchStateKey)!;
const { executions, project, backendApi, monacoApi, activeTab } = workbenchState;

const treeRef = useTemplateRef("treeRef");

const activeEntry = ref<FileSystemNode | ExecutionWithLoadedTree>();
const expandedItems = ref<Set<FileSystemNode | ExecutionWithLoadedTree>>(new Set());

const showDeleteDialog = ref(false);
const executionToDelete = ref<ExecutionWithLoadedTree | null>(null);
const showDeleteAllDialog = ref(false);
const executionsList = computed(() => {
    return Array.from(executions.value.values()).sort((a, b) => {
        return b.execution.name.localeCompare(a.execution.name);
    });
});

watch(
    activeTab,
    async (newTab) => {
        if (!newTab) {
            activeEntry.value = undefined;
            return;
        }

        const parsed = parseUri(newTab.fileUri);

        if (parsed.category === FileCategory.ExecutionSummary) {
            await handleExecutionSummarySelection(parsed.executionId);
        } else if (parsed.category === FileCategory.ExecutionResultFile) {
            await handleExecutionFileSelection(parsed.executionId, parsed.path);
        } else {
            activeEntry.value = undefined;
        }
    },
    { immediate: true }
);

watch(activeEntry, () => {
    nextTick(() => {
        const element = treeRef.value!.$el.querySelector(`[data-active=true]`) as HTMLElement;
        if (element != undefined) {
            const rect = element.getBoundingClientRect();
            const completelyVisible =
                rect.top >= 0 && rect.bottom <= (window.innerHeight || document.documentElement.clientHeight);
            if (!completelyVisible) {
                element.scrollIntoView({ block: "center", behavior: "instant" });
            }
        }
    });
});

async function handleExecutionSummarySelection(executionId: string) {
    const executionData = executions.value.get(executionId);
    if (executionData == undefined) {
        activeEntry.value = undefined;
        return;
    }

    activeEntry.value = executionData
}

async function handleExecutionFileSelection(executionId: string, filePath: string) {
    const executionData = executions.value.get(executionId);
    if (!executionData) {
        activeEntry.value = undefined;
        return;
    }

    if (executionData.fileTree == undefined && !executionData.isLoadingTree) {
        await workbenchState.loadExecutionFileTree(executionId);
    }

    if (executionData.fileTree == undefined) {
        activeEntry.value = undefined;
        return;
    }

    const fileNode = findFileInTree(executionData.fileTree, filePath);
    if (fileNode == undefined) {
        activeEntry.value = undefined;
        return;
    }

    activeEntry.value = fileNode;

    expandedItems.value.add(fileNode);

    let parent = fileNode.parent;
    while (parent != null) {
        expandedItems.value.add(parent);
        parent = parent.parent;
    }
    expandedItems.value.add(executionData);
}

function handleSelect(entry: FileSystemNode | ExecutionWithLoadedTree) {
    activeEntry.value = entry;
}

async function handleCancel(executionId: string) {
    if (project.value == undefined) {
        return;
    }

    const result = await backendApi.executions.cancel(project.value.id, executionId);
    if (!result.success) {
        showApiError("cancel execution", result.error.message);
    }
}

function handleDelete(executionData: ExecutionWithLoadedTree) {
    executionToDelete.value = executionData;
    showDeleteDialog.value = true;
}

async function confirmDelete() {
    if (project.value == undefined || executionToDelete.value == undefined) {
        return;
    }

    const executionId = executionToDelete.value.execution.id;
    const result = await backendApi.executions.delete(project.value.id, executionId);
    if (!result.success) {
        showApiError("delete execution", result.error.message);
    } else {
        executions.value.delete(executionId);
    }

    executionToDelete.value = null;
    showDeleteDialog.value = false;
}

function handleDeleteAll() {
    showDeleteAllDialog.value = true;
}

async function confirmDeleteAll() {
    if (project.value == undefined) {
        return;
    }

    const result = await backendApi.executions.deleteAll(project.value.id);
    if (!result.success) {
        showApiError("delete all executions", result.error.message);
    } else {
        executions.value.clear();
    }

    showDeleteAllDialog.value = false;
}

onActivated(async () => {
    await workbenchState.refreshExecutions();
});
</script>
<style scoped>
.executions-container :deep(div[data-reka-scroll-area-viewport] > div:first-child) {
    @apply min-h-full flex flex-col;
}
</style>
