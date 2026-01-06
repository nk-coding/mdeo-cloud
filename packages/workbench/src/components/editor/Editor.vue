<template>
    <ResizablePanelGroup v-show="languagePlugin != undefined" direction="horizontal">
        <ResizablePanel>
            <div ref="editorElement" class="h-full w-full"></div>
        </ResizablePanel>
        <ResizableHandle :class="{ hidden: !hasEditor }" />
        <ResizablePanel :class="{ hidden: !hasEditor }">
            <GraphicalEditor v-for="tab in tabs" :key="tab.file.id.toString()" v-show="tab === activeTab" :tab="tab" />
        </ResizablePanel>
    </ResizablePanelGroup>
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

const { tabs, activeTab, languagePluginByExtension, monacoApi, fileTree } = inject(workbenchStateKey)!;
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
</script>
