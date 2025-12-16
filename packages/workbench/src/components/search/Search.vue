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
import { inject, ref, shallowRef, nextTick, onActivated } from "vue";
import { Input } from "../ui/input";
import { Toggle } from "../ui/toggle";
import { TooltipProvider, Tooltip, TooltipContent, TooltipTrigger } from "../ui/tooltip";
import { throttledWatch } from "@vueuse/core";
import {
    QueryType,
    type ITextSearchMatch,
    type SearchRangeSetPairing
} from "@codingame/monaco-vscode-api/vscode/vs/workbench/services/search/common/search";
import { Uri } from "vscode";
import { workbenchStateKey } from "../workbench/util";
import Tree from "@/components/tree/Tree.vue";
import TreeItem from "@/components/tree/TreeItem.vue";
import ScrollArea from "../ui/scroll-area/ScrollArea.vue";
import { File, CaseSensitive, WholeWord, Regex } from "lucide-vue-next";
import { FileType } from "@codingame/monaco-vscode-files-service-override";
import type { TreeItem as TreeItemType } from "@/components/tree/util";
import { findFileInTree } from "@/data/filesystem/util";

interface SearchMatch extends TreeItemType {
    id: string;
    previewText?: string;
    range?: SearchRangeSetPairing;
    fileResult: FileSearchResult;
    index: number;
}

interface FileSearchResult extends TreeItemType {
    id: string;
    resource: Uri;
    results?: SearchMatch[];
}

const workbenchState = inject(workbenchStateKey)!;
const { monacoApi, fileTree, project } = workbenchState;

const searchText = ref("");
const isCaseSensitive = ref(false);
const isWholeWord = ref(false);
const isRegex = ref(false);
const searchResults = shallowRef<FileSearchResult[]>([]);
const activeElement = ref<SearchMatch | FileSearchResult>();
const expandedItems = ref<Set<SearchMatch | FileSearchResult>>(new Set());

async function performSearch() {
    if (searchText.value.trim() === "" || project.value == undefined) {
        searchResults.value = [];
        expandedItems.value.clear();
        return;
    }

    const searchResult = await monacoApi.searchService.textSearch({
        type: QueryType.Text,
        contentPattern: {
            pattern: searchText.value,
            isRegExp: isRegex.value,
            isCaseSensitive: isCaseSensitive.value,
            isWordMatch: isWholeWord.value
        },
        folderQueries: [
            {
                folder: Uri.parse("file:///")
            }
        ]
    });

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
            const fileResult: FileSearchResult = {
                id: result.resource.path,
                resource: result.resource,
                results: undefined
            };

            fileResult.results =
                (result.results as ITextSearchMatch<Uri>[] | undefined)?.map((match, index) => ({
                    id: `${result.resource.path}-${index}`,
                    previewText: match.previewText,
                    range: match.rangeLocations[0],
                    fileResult,
                    index
                })) ?? [];

            return fileResult;
        });

    expandedItems.value = new Set(searchResults.value);
}

throttledWatch([searchText, isCaseSensitive, isWholeWord, isRegex], performSearch, {
    throttle: 300,
    leading: true,
    trailing: true
});

onActivated(async () => {
    await performSearch();
});

function getFileName(uri: Uri): string {
    const pathSegments = uri.path.split("/").filter((s) => s.length > 0);
    return pathSegments[pathSegments.length - 1] ?? "";
}

function getRelativePath(uri: Uri): string {
    const pathSegments = uri.path.split("/").filter((s) => s.length > 0);
    if (pathSegments.length <= 2) {
        return "";
    }
    return pathSegments.slice(1, -1).join("/");
}

function getPreviewBefore(match: SearchMatch): string {
    if (!match.previewText || !match.range) {
        return "";
    }
    const startOffset = match.range.preview.startColumn;
    return match.previewText.substring(0, startOffset);
}

function getPreviewHighlight(match: SearchMatch): string {
    if (!match.previewText || !match.range) {
        return "";
    }
    const startOffset = match.range.preview.startColumn;
    const endOffset = match.range.preview.endColumn;
    return match.previewText.substring(startOffset, endOffset);
}

function getPreviewAfter(match: SearchMatch): string {
    if (!match.previewText || !match.range) {
        return "";
    }
    const endOffset = match.range.preview.endColumn;
    return match.previewText.substring(endOffset);
}

async function handleSelectResult(match: SearchMatch, temporary: boolean) {
    activeElement.value = match;

    const file = findFileInTree(fileTree, match.fileResult.resource);

    if (file != undefined && file.type === FileType.File) {
        workbenchState.openTab(file, temporary);

        await nextTick();
        // TODO: Use match.range() to navigate to the specific line/column if needed
    }
}
</script>
<style scoped>
.search-tree-container :deep(div[data-reka-scroll-area-viewport] > div:first-child) {
    @apply min-h-full flex flex-col;
}
</style>
