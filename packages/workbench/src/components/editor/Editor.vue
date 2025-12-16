<template>
    <ResizablePanelGroup v-show="languageId != undefined" direction="horizontal">
        <ResizablePanel>
            <div ref="editorElement" class="h-full w-full"></div>
        </ResizablePanel>
        <template v-if="true">
            <ResizableHandle />
            <ResizablePanel> <div class="m-4">Graphical editor TODO</div> </ResizablePanel>
        </template>
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

const { tabs, activeTab, languagePlugins: fileTypePlugins, monacoApi } = inject(workbenchStateKey)!;
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

const languageId = computed(() => {
    const tab = activeTab.value;
    if (tab == undefined) {
        return undefined;
    }
    const extension = `.${tab.file.id.path.split(".").pop()}`;
    return fileTypePlugins.value.find((plugin) => plugin.extension === extension)?.id;
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

onMounted(() => {
    const monacoEditor = monacoApi.monaco.editor.create(editorElement.value!);
    monacoEditor.layout();
    editor.value = monacoEditor;
});
</script>
