<template>
    <ResizablePanelGroup v-show="fileType != undefined" direction="horizontal">
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
import { computed, inject, onMounted, shallowRef, useTemplateRef } from "vue";
import * as monaco from "monaco-editor";
import { monacoReadyKey } from "@/plugins/monacoPlugin";
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "../ui/resizable";
import { useResizeObserver } from "@vueuse/core";
import { workbenchStateKey } from "@/data/workbenchState";

const monacoReady = inject(monacoReadyKey)!;
const workbenchState = inject(workbenchStateKey)!;
const editorElement = useTemplateRef("editorElement");
const editor = shallowRef<monaco.editor.IStandaloneCodeEditor>();

const fileType = computed(() => {
    const state = workbenchState.value;
    const activeTab = state.activeTab.value;
    if (!activeTab) {
        return undefined;
    }
    return state.supportedFileTypes.value.find((fileType) => fileType.id === activeTab.file.fileType);
});

useResizeObserver(editorElement, () => {
    const monacoEditor = editor.value;
    if (monacoEditor != undefined) {
        monacoEditor.layout();
    }
});

onMounted(async () => {
    await monacoReady;

    const editorAppConfig: EditorAppConfig = {
        editorOptions: {
            model: null
        },
        overrideAutomaticLayout: false
    };

    const editorApp = new EditorApp(editorAppConfig);
    await editorApp.start(editorElement.value!);
    const monacoEditor = editorApp.getEditor()!;
    monacoEditor.layout();
    editor.value = monacoEditor;
});
</script>
