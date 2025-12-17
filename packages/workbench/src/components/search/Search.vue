<template>
    <div class="flex flex-col h-full">
        <div class="px-3 pt-2 pb-2">
            <div class="relative">
                <Input v-model="searchText" placeholder="Search..." class="pr-23" />
                <div class="absolute right-1 top-1/2 -translate-y-1/2 flex items-center gap-0.5">
                    <TooltipProvider>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <div>
                                    <Toggle v-model="isCaseSensitive" class="h-7 min-w-7 px-0">
                                        <CaseSensitive c />
                                    </Toggle>
                                </div>
                            </TooltipTrigger>
                            <TooltipContent side="top"> Match Case </TooltipContent>
                        </Tooltip>
                    </TooltipProvider>
                    <TooltipProvider>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <div>
                                    <Toggle v-model="isWholeWord" class="h-7 min-w-7 px-0">
                                        <WholeWord c />
                                    </Toggle>
                                </div>
                            </TooltipTrigger>
                            <TooltipContent side="top"> Match Whole Word </TooltipContent>
                        </Tooltip>
                    </TooltipProvider>
                    <TooltipProvider>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <div>
                                    <Toggle v-model="isRegex" class="h-7 min-w-7 px-0">
                                        <Regex c />
                                    </Toggle>
                                </div>
                            </TooltipTrigger>
                            <TooltipContent side="top"> Use Regular Expression </TooltipContent>
                        </Tooltip>
                    </TooltipProvider>
                </div>
            </div>
        </div>
        <ScrollArea class="search-tree-container flex-1 min-h-0 w-full">
            <Tree class="flex-1 w-full p-2" :active-element="activeElement" :expanded-items="expandedItems">
                <TreeItem
                    v-for="fileResult in searchResults"
                    :key="fileResult.id"
                    :data="fileResult"
                    :is-folder="true"
                    :has-children="(fileResult.results?.length ?? 0) > 0"
                    @focus="activeElement = fileResult"
                >
                    <template #content>
                        <File class="w-4 h-4 mr-2" />
                        <span>{{ getFileName(fileResult.resource) }}</span>
                        <span class="ml-2 text-xs opacity-80">{{ getRelativePath(fileResult.resource) }}</span>
                    </template>
                    <template #items>
                        <TreeItem
                            v-for="match in fileResult.results ?? []"
                            :key="match.id"
                            :data="match"
                            :is-folder="false"
                            :has-children="false"
                            @focus="handleSelectResult(match, true)"
                            @dblclick="handleSelectResult(match, false)"
                        >
                            <template #content>
                                <span class="text-sm truncate">
                                    <template v-if="match.range && match.previewText">
                                        {{ getPreviewBefore(match)
                                        }}<span
                                            class="bg-yellow-200 dark:bg-yellow-800 group-focus/tree-button:bg-yellow-800 group-focus-within/tree:group-data-[active=true]/tree-button:bg-yellow-800"
                                            >{{ getPreviewHighlight(match) }}</span
                                        >{{ getPreviewAfter(match) }}
                                    </template>
                                    <template v-else>{{ match.previewText ?? "" }}</template>
                                </span>
                            </template>
                        </TreeItem>
                    </template>
                </TreeItem>
            </Tree>
        </ScrollArea>
    </div>
</template>
<script setup lang="ts">
import { inject, ref, shallowRef, onActivated, onMounted, onDeactivated, computed } from "vue";
import { Input } from "../ui/input";
import { Toggle } from "../ui/toggle";
import { TooltipProvider, Tooltip, TooltipContent, TooltipTrigger } from "../ui/tooltip";
import { throttledWatch } from "@vueuse/core";
import type { ITextSearchMatch } from "@codingame/monaco-vscode-api/vscode/vs/workbench/services/search/common/search";
import { Uri } from "vscode";
import { workbenchStateKey } from "../workbench/util";
import Tree from "@/components/tree/Tree.vue";
import TreeItem from "@/components/tree/TreeItem.vue";
import ScrollArea from "../ui/scroll-area/ScrollArea.vue";
import { File, CaseSensitive, WholeWord, Regex } from "lucide-vue-next";
import { FileType } from "@codingame/monaco-vscode-files-service-override";
import { findFileInTree } from "@/data/filesystem/util";
import type { SearchMatch, FileSearchResult } from "./types";
import {
    createSearchQuery,
    createFileSearchResult,
    getFileName,
    getRelativePath,
    getPreviewBefore,
    getPreviewHighlight,
    getPreviewAfter
} from "./util";

const { monacoApi, fileTree, project, activeTab } = inject(workbenchStateKey)!;

const searchText = ref("");
const isCaseSensitive = ref(false);
const isWholeWord = ref(false);
const isRegex = ref(false);
const searchResults = shallowRef<FileSearchResult[]>([]);
const activeElement = ref<SearchMatch | FileSearchResult>();
const expandedItems = ref<Set<SearchMatch | FileSearchResult>>(new Set());
const isSearchActive = ref(true);

const shouldPerformSerach = computed(
    () => isSearchActive.value && searchText.value.trim() !== "" && project.value != undefined
);

async function searchSingleFile(uri: Uri): Promise<FileSearchResult | null> {
    if (!shouldPerformSerach.value) {
        return null;
    }

    try {
        const relativePath = uri.path.startsWith("/") ? uri.path.substring(1) : uri.path;

        const searchResult = await monacoApi.searchService.textSearch(
            createSearchQuery(searchText.value, isRegex.value, isCaseSensitive.value, isWholeWord.value, relativePath)
        );

        const fileResult = searchResult.results.find((result) => result.resource.path === uri.path);

        if (fileResult && fileResult.results && fileResult.results.length > 0) {
            return createFileSearchResult(
                fileResult.resource,
                fileResult.results as ITextSearchMatch<Uri>[] | undefined
            );
        }
    } catch {
        return null;
    }

    return null;
}

async function performSearch() {
    if (!shouldPerformSerach.value) {
        searchResults.value = [];
        expandedItems.value.clear();
        return;
    }

    const searchResult = await monacoApi.searchService.textSearch(
        createSearchQuery(searchText.value, isRegex.value, isCaseSensitive.value, isWholeWord.value)
    );

    const processedPaths = new Set<string>();
    searchResults.value = searchResult.results
        .filter((result) => {
            if (processedPaths.has(result.resource.path)) {
                return false;
            }
            processedPaths.add(result.resource.path);
            return true;
        })
        .map((result): FileSearchResult => {
            return createFileSearchResult(result.resource, result.results as ITextSearchMatch<Uri>[] | undefined);
        });

    expandedItems.value = new Set(searchResults.value);
}

async function updateFileSearchResults(changedUris: Uri[]) {
    if (!shouldPerformSerach.value) {
        return;
    }

    const updatedResults = [...searchResults.value];
    let resultsChanged = false;

    for (const uri of changedUris) {
        const existingIndex = updatedResults.findIndex((r) => r.resource.path === uri.path);
        const newResult = await searchSingleFile(uri);

        if (newResult && newResult.results && newResult.results.length > 0) {
            if (existingIndex !== -1) {
                updatedResults[existingIndex] = newResult;
            } else {
                updatedResults.push(newResult);
            }
            resultsChanged = true;
        } else if (existingIndex !== -1) {
            updatedResults.splice(existingIndex, 1);
            resultsChanged = true;
        }
    }

    if (resultsChanged) {
        searchResults.value = updatedResults;

        const newExpandedItems = new Set<SearchMatch | FileSearchResult>();
        for (const item of expandedItems.value) {
            if ("resource" in item) {
                const updatedResult = updatedResults.find((r) => r.resource.path === item.resource.path);
                if (updatedResult) {
                    newExpandedItems.add(updatedResult);
                }
            }
        }
        expandedItems.value = newExpandedItems;
    }
}

throttledWatch([searchText, isCaseSensitive, isWholeWord, isRegex], performSearch, {
    throttle: 300,
    leading: true,
    trailing: true
});

onMounted(() => {
    monacoApi.fileService.onDidRunOperation(() => {
        if (!shouldPerformSerach.value) {
            return;
        }
        performSearch();
    });

    monacoApi.fileService.onDidFilesChange((event) => {
        if (!shouldPerformSerach.value) {
            return;
        }

        const changedUris: Uri[] = [];
        const rawChanges = (event as any).raw;
        if (Array.isArray(rawChanges)) {
            for (const change of rawChanges) {
                if (change.resource) {
                    changedUris.push(change.resource);
                }
            }
        }

        if (changedUris.length > 0) {
            updateFileSearchResults(changedUris);
        }
    });
});

onActivated(async () => {
    isSearchActive.value = true;
    await performSearch();
});

onDeactivated(() => {
    isSearchActive.value = false;
});

async function handleSelectResult(match: SearchMatch, temporary: boolean) {
    activeElement.value = match;

    const file = findFileInTree(fileTree, match.fileResult.resource);

    if (file != undefined && file.type === FileType.File) {
        const range = match.range?.source;
        await monacoApi.editorService.openEditor({
            resource: file.id,
            options: {
                preserveFocus: true,
                selection:
                    range != undefined
                        ? {
                              startLineNumber: range.startLineNumber + 1,
                              startColumn: range.startColumn + 1,
                              endLineNumber: range.endLineNumber + 1,
                              endColumn: range.endColumn + 1
                          }
                        : undefined
            }
        });
        if (!temporary && activeTab.value != undefined) {
            activeTab.value.temporary = false;
        }
    }
}
</script>
<style scoped>
.search-tree-container :deep(div[data-reka-scroll-area-viewport] > div:first-child) {
    @apply min-h-full flex flex-col;
}
</style>
