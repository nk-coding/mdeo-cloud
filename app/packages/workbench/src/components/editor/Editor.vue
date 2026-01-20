<template>
    <div class="w-full h-full">
        <ResizablePanelGroup v-show="languagePlugin != undefined" direction="horizontal">
            <ResizablePanel>
                <div ref="editorElement" class="h-full w-full"></div>
            </ResizablePanel>
            <ResizableHandle :class="{ hidden: !hasEditor }" />
            <ResizablePanel :class="{ hidden: !hasEditor }">
                <GraphicalEditor
                    v-for="tab in tabs"
                    :key="tab.file.id.toString()"
                    v-show="tab === activeTab"
                    :tab="tab"
                />
            </ResizablePanel>
        </ResizablePanelGroup>
        <div
            v-show="languagePlugin === undefined && activeTab != undefined"
            class="h-full flex items-center justify-center p-8"
        >
            <div class="flex flex-col items-center gap-4 max-w-md text-center">
                <AlertCircle class="w-12 h-12 text-muted-foreground" />
                <div class="space-y-2">
                    <h3 class="text-lg font-semibold text-foreground">No Language Support Available</h3>
                    <p class="text-sm text-muted-foreground">
                        No language support for the file extension
                        <span class="font-mono font-medium">{{ activeTab?.file.extension }}</span> is currently
                        available.
                    </p>
                </div>
                <Button @click="openManagePlugins">
                    <Settings class="w-4 h-4 mr-2" />
                    Manage Plugins
                </Button>
            </div>
        </div>

        <ManagePluginsDialog
            v-if="project != undefined"
            v-model:open="isManagePluginsDialogOpen"
            :project-id="project.id"
        />
    </div>
</template>
<script setup lang="ts">
import { computed, inject, onMounted, shallowRef, useTemplateRef, watch } from "vue";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "../ui/resizable";
import { useResizeObserver, watchArray } from "@vueuse/core";
import * as monacoType from "monaco-editor";
import { workbenchStateKey } from "../workbench/util";
import type { EditorTab } from "@/data/tab/editorTab";
import { EditorState } from "./util";
import type { File } from "@/data/filesystem/file";
import { findFileInTree } from "@/data/filesystem/util";
import GraphicalEditor from "./GraphicalEditor.vue";
import { AlertCircle, Settings } from "lucide-vue-next";
import { Button } from "@/components/ui/button";
import ManagePluginsDialog from "../projects/ManagePluginsDialog.vue";

const { tabs, activeTab, languagePluginByExtension, monacoApi, fileTree, project } = inject(workbenchStateKey)!;
const editorElement = useTemplateRef("editorElement");
const editor = shallowRef<monacoType.editor.IStandaloneCodeEditor>();

const editorStates = new Map<EditorTab, EditorState>();

watchArray(
    tabs,
    (_new, _old, _added, removed) => {
        for (const tab of removed) {
            const state = editorStates.get(tab);
            if (state != undefined) {
                state.dispose();
                editorStates.delete(tab);
            }
        }
    },
    {
        deep: true
    }
);

const activeTabUri = computed(() => {
    return activeTab.value?.file.id;
});

const languagePlugin = computed(() => {
    const tab = activeTab.value;
    if (tab == undefined) {
        return undefined;
    }
    return languagePluginByExtension.value.get(tab.file.extension);
});

const languageId = computed(() => {
    const plugin = languagePlugin.value;
    return plugin?.id;
});

const hasEditor = computed(() => {
    const plugin = languagePlugin.value;
    return plugin?.editorPlugin != undefined;
});

watch(
    [activeTab, activeTabUri, languageId],
    async ([newTab, newTabUri, newLanguageId], [oldTab, oldTabUri, oldLanguageId]) => {
        if (newTab == undefined || newTabUri == undefined || newLanguageId == undefined) {
            editor.value?.setModel(null);
            return;
        }
        const oldEditorState = oldTab != undefined ? editorStates.get(oldTab) : undefined;
        let newEditorState = editorStates.get(newTab);
        if (newEditorState == undefined) {
            newEditorState = new EditorState(newTab.file.id);
            await newEditorState.updateUri(newTab.file.id, newLanguageId);
            editorStates.set(newTab, newEditorState);
        } else if (newTabUri !== oldTabUri || newLanguageId !== oldLanguageId) {
            await newEditorState.updateUri(newTabUri, newLanguageId);
        }
        if (oldEditorState != undefined && editor.value != undefined) {
            oldEditorState.viewState = editor.value.saveViewState() ?? undefined;
        }
        if (editor.value != undefined) {
            editor.value.setModel(newEditorState.modelReference!.object.textEditorModel);
            if (newEditorState.viewState != undefined) {
                editor.value.restoreViewState(newEditorState.viewState);
            }
        }
    },
    {
        immediate: true,
        deep: true
    }
);

useResizeObserver(editorElement, () => {
    const monacoEditor = editor.value;
    if (monacoEditor != undefined) {
        monacoEditor.layout();
    }
});

function openTab(file: File, temporary: boolean) {
    const existingTab = tabs.value.find((tab) => tab.file.id.toString() === file.id.toString());

    if (existingTab) {
        activeTab.value = existingTab;
        if (!temporary && existingTab.temporary) {
            existingTab.temporary = false;
        }
    } else {
        const newTab = {
            file: file,
            temporary: temporary
        };
        tabs.value.push(newTab);
        activeTab.value = newTab;
    }
}

onMounted(() => {
    const monacoEditor = monacoApi.monaco.editor.create(editorElement.value!);
    monacoEditor.updateOptions({
        hover: {
            above: false
        },
        glyphMargin: false,
        fixedOverflowWidgets: true,
        automaticLayout: false
    })
    monacoEditor.layout();
    editor.value = monacoEditor;
    monacoApi.openEditorFunc = async (createModelReference, options) => {
        openTab(
            findFileInTree(fileTree, createModelReference.object.textEditorModel.uri) as File,
            options?.pinned !== true
        );
        return monacoEditor;
    };
});

const isManagePluginsDialogOpen = shallowRef(false);

function openManagePlugins() {
    // If user is admin, we could open settings, but for now just open project's manage plugins
    if (project.value) {
        isManagePluginsDialogOpen.value = true;
    }
}
</script>
