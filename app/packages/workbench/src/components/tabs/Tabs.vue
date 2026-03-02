<template>
    <template v-if="tabs.length > 0">
        <div class="flex items-center w-full min-w-0">
            <ScrollArea orientation="horizontal" class="flex-1 min-w-0" @wheel="handleWheel">
                <TabsRoot v-model="activeTabPath">
                    <TabsList
                        ref="tabsRef"
                        class="text-muted-foreground inline-flex w-fit items-center justify-start rounded-lg gap-1 m-1.5"
                    >
                        <FileTab
                            v-for="tab in tabs"
                            :key="getTabKey(tab)"
                            :tab="tab"
                            :is-active="activeTab === tab"
                            @close="closeTab"
                            @close-others="closeOtherTabs"
                            @close-to-right="closeTabsToRight"
                            @close-all="closeAllTabs"
                        />
                    </TabsList>
                </TabsRoot>
            </ScrollArea>
            <div v-if="editorTitleActions.length > 0" class="flex items-center gap-1 px-2 shrink-0">
                <TooltipProvider>
                    <Tooltip v-for="action in editorTitleActions" :key="action.key">
                        <TooltipTrigger as-child>
                            <Button variant="ghost" size="icon" class="size-7" @click="handleActionClick(action)">
                                <Icon :iconNode="action.icon" :name="action.key" class="size-4" />
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                            <p>{{ action.name }}</p>
                        </TooltipContent>
                    </Tooltip>
                </TooltipProvider>
            </div>
        </div>
        <Separator />
    </template>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onMounted, onUnmounted, ref, useTemplateRef, watch } from "vue";
import { TabsRoot, TabsList } from "reka-ui";
import { Icon } from "lucide-vue-next";
import FileTab from "./FileTab.vue";
import type { EditorTab } from "@/data/tab/editorTab";
import { Separator } from "../ui/separator";
import { Button } from "../ui/button";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "../ui/tooltip";
import ScrollArea from "../ui/scroll-area/ScrollArea.vue";
import { workbenchStateKey } from "../workbench/util";
import { watchArray } from "@vueuse/core";
import { ActionDisplayLocation, type FileAction } from "@mdeo/language-common";
import { getFileExtension } from "@/data/filesystem/util";
import {
    FileOperation,
    type FileOperationEvent
} from "@codingame/monaco-vscode-api/vscode/vs/platform/files/common/files";
import type { IDisposable } from "@codingame/monaco-vscode-api/vscode/vs/base/common/lifecycle";
import { fetchFileActions as fetchAvailableFileActions, triggerFileAction } from "@/components/action/fileActions";

const workbenchState = inject(workbenchStateKey)!;
const { tabs, activeTab, languageClient, languagePluginByExtension, pendingAction, monacoApi } = workbenchState;

const tabsRef = useTemplateRef("tabsRef");

/**
 * All actions available for the active file
 */
const fileActions = ref<FileAction[]>([]);

/**
 * Actions to display in the editor title area
 */
const editorTitleActions = computed(() =>
    fileActions.value.filter((action) => action.displayLocations.includes(ActionDisplayLocation.EDITOR_TITLE))
);

/**
 * Gets a unique key for a tab for Vue's v-for
 *
 * @param tab The tab to get a key for
 * @returns A unique string key for the tabe
 */
function getTabKey(tab: EditorTab): string {
    return `file:${tab.fileUri.toString()}`;
}

const activeTabPath = computed({
    get: () => {
        const tab = activeTab.value;
        if (tab == undefined) return undefined;
        return getTabKey(tab);
    },
    set: (path: string | undefined) => {
        if (path == undefined) return;
        const tab = tabs.value.find((t) => getTabKey(t) === path);
        if (tab != undefined) {
            activeTab.value = tab;
        }
    }
});

/**
 * Watches the active tab and fetches available actions for the file
 */
watch(
    activeTab,
    async (newTab) => {
        if (newTab != undefined) {
            nextTick(() => {
                const tabsElement = tabsRef.value!.$el as HTMLElement | undefined;
                const activeTabElement = tabsElement?.querySelector('[data-state="active"]') as HTMLElement | undefined;
                if (activeTabElement != undefined) {
                    activeTabElement.scrollIntoView({ block: "nearest", behavior: "instant" });
                }
            });

            await fetchFileActions(newTab);
        } else {
            fileActions.value = [];
        }
    },
    { immediate: true }
);

/**
 * Fetches available actions for a file from the language server
 *
 * @param tab The file tab to fetch actions for
 */
async function fetchFileActions(tab: EditorTab): Promise<void> {
    const fileUri = tab.fileUri.toString();
    const fileName = tab.fileUri.path.split("/").pop() || "";
    const extension = getFileExtension(fileName);

    fileActions.value = await fetchAvailableFileActions(
        {
            languageClient,
            languagePluginByExtension
        },
        fileUri,
        extension
    );
}

/**
 * Handles clicking on an action button
 *
 * @param action The action that was clicked
 */
function handleActionClick(action: FileAction): void {
    const tab = activeTab.value;
    if (tab == undefined) {
        return;
    }

    const fileName = tab.fileUri.path.split("/").pop() ?? "";
    const extension = getFileExtension(fileName);

    triggerFileAction(pendingAction, languagePluginByExtension, action, tab.fileUri.toString(), extension);
}

function shouldRefetchActions(event: FileOperationEvent): boolean {
    const tab = activeTab.value;
    if (tab == undefined) {
        return false;
    }

    if (event.operation !== FileOperation.WRITE) {
        return false;
    }

    return event.resource.toString() === tab.fileUri.toString();
}

let fileOperationListener: IDisposable | undefined;

onMounted(() => {
    fileOperationListener = monacoApi.fileService.onDidRunOperation((event) => {
        if (!shouldRefetchActions(event)) {
            return;
        }

        const tab = activeTab.value;
        if (tab != undefined) {
            void fetchFileActions(tab);
        }
    });
});

onUnmounted(() => {
    fileOperationListener?.dispose();
    fileOperationListener = undefined;
});

/**
 * Closes a tab and selects a new active tab if needed
 *
 * @param tab The tab to close
 */
function closeTab(tab: EditorTab) {
    const currentTabs = tabs.value;
    const index = currentTabs.findIndex((t) => t === tab);

    if (index === -1) {
        return;
    }

    tabs.value = currentTabs.filter((t) => t !== tab);

    if (activeTab.value === tab) {
        const remainingTabs = tabs.value;
        if (remainingTabs.length > 0) {
            const newIndex = Math.min(index, remainingTabs.length - 1);
            activeTab.value = remainingTabs[newIndex];
        } else {
            activeTab.value = undefined;
        }
    }
}

/**
 * Closes all tabs except the specified one
 *
 * @param tab The tab to keep open
 */
function closeOtherTabs(tab: EditorTab) {
    tabs.value = [tab];
    activeTab.value = tab;
}

/**
 * Closes all tabs to the right of the specified tab
 *
 * @param tab The tab to the left of which all tabs should be closed
 */
function closeTabsToRight(tab: EditorTab) {
    const currentTabs = tabs.value;
    const index = currentTabs.findIndex((t) => t === tab);

    if (index === -1 || index === currentTabs.length - 1) {
        return;
    }

    const tabsToClose = currentTabs.slice(index + 1);
    tabs.value = currentTabs.slice(0, index + 1);

    if (activeTab.value && tabsToClose.includes(activeTab.value)) {
        activeTab.value = tabs.value[tabs.value.length - 1];
    }
}

/**
 * Closes all tabs
 */
function closeAllTabs() {
    tabs.value = [];
    activeTab.value = undefined;
}

/**
 * Handles horizontal scrolling in the tabs area
 *
 * @param event The wheel event
 */
function handleWheel(event: WheelEvent) {
    if (event.deltaY !== 0) {
        const target = event.currentTarget as HTMLElement;
        const scrollViewport = target.querySelector('[data-slot="scroll-area-viewport"]') as HTMLElement;

        if (scrollViewport) {
            event.preventDefault();
            scrollViewport.scrollLeft += event.deltaY;
        }
    }
}

watchArray(
    tabs,
    (newTabs, oldTabs) => {
        const oldTemporaryTabs = oldTabs.filter((tab) => tab.temporary);
        const newTemporaryTabs = newTabs.filter((tab) => tab.temporary);
        if (newTemporaryTabs.length > oldTemporaryTabs.length) {
            for (const tab of oldTemporaryTabs) {
                closeTab(tab);
            }
        }
    },
    { deep: true }
);
</script>
