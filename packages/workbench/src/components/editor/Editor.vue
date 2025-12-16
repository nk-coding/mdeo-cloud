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
import { EditorApp, type EditorAppConfig } from "monaco-languageclient/editorApp";
import { computed, inject, onMounted, shallowRef, useTemplateRef, watch } from "vue";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "../ui/resizable";
import { useResizeObserver, watchArray } from "@vueuse/core";
import * as monacoType from "monaco-editor";
import { workbenchStateKey } from "../workbench/util";
import type { IDisposable } from "monaco-editor";
import type { Uri } from "vscode";
import type { EditorTab } from "@/data/tab/editorTab";
import { createModelReference, type IReference, type ITextFileEditorModel } from "@codingame/monaco-vscode-api/monaco";

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
    async ([newTab, newTabUri, newLanguageId], [_oldTab, oldTabUri, oldLanguageId]) => {
        if (newTab == undefined || newTabUri == undefined || newLanguageId == undefined) {
            editor.value?.setModel(null);
            return;
        }
        let editorState = editorStates.get(newTab);
        if (editorState == undefined) {
            editorState = new EditorState(newTab.file.id);
            await editorState.updateUri(newTab.file.id, newLanguageId);
            editorStates.set(newTab, editorState);
        } else if (newTabUri !== oldTabUri || newLanguageId !== oldLanguageId) {
            await editorState.updateUri(newTabUri, newLanguageId);
        }
        if (editor.value != undefined) {
            editor.value.setModel(editorState.modelReference!.object.textEditorModel);
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

class EditorState implements IDisposable {
    modelReference: IReference<ITextFileEditorModel> | undefined;

    constructor(public uri: Uri) {}

    async updateUri(newUri: Uri, language: string) {
        this.uri = newUri;
        this.modelReference?.dispose();
        this.modelReference = await createModelReference(newUri);
        this.modelReference.object.setLanguageId(language);
    }

    dispose(): void {
        this.modelReference?.dispose();
    }
}
</script>
