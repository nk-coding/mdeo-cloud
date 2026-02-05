<template>
    <div
        ref="sprottyWrapper"
        class="sprotty-wrapper w-full h-full relative ml-1.5"
        :class="graphicalEditorPlugin?.stylesCls"
    >
        <div v-if="graphicalEditorPlugin != undefined" :id="id"></div>
    </div>
</template>
<script setup lang="ts">
import "reflect-metadata";
import { createContainer } from "@mdeo/editor-common";
import { computed, inject, onMounted, ref, shallowRef, useId } from "vue";
import type { EditorTab } from "@/data/tab/editorTab";
import { workbenchStateKey } from "../workbench/util";
import { MonacoGLSPClient } from "./glspClient";
import { DiagramLoader } from "@eclipse-glsp/client";
import { editorContextKey } from "@/lib/editorPlugin";
import { useResizeObserver } from "@vueuse/core";
import type { ResetCanvasBoundsAction } from "@mdeo/editor-protocol";
import type { IActionDispatcher } from "@eclipse-glsp/sprotty";
import { EditMode, TYPES } from "@eclipse-glsp/sprotty";
import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";

const props = defineProps<{
    tab: EditorTab;
    languagePlugin: ResolvedWorkbenchLanguagePlugin;
}>();

const { languageClient } = inject(workbenchStateKey)!;
const editorContext = inject(editorContextKey)!;

const id = useId();
const sprottyWrapper = ref<HTMLElement | null>(null);
const actionDispatcher = shallowRef<IActionDispatcher>();

const graphicalEditorPlugin = computed(() => {
    return props.languagePlugin.graphicalEditorPlugin;
});

useResizeObserver(sprottyWrapper, () => {
    actionDispatcher.value?.dispatch({ kind: "resetCanvasBoundsAction" } satisfies ResetCanvasBoundsAction);
});

onMounted(async () => {
    if (props.languagePlugin.graphicalEditorPlugin == undefined) {
        return;
    }

    const client = new MonacoGLSPClient({
        client: languageClient.value!,
        id: id
    });

    const plugin = graphicalEditorPlugin.value;
    if (plugin == undefined) {
        return undefined;
    }
    const container = createContainer(editorContext, plugin.containerConfiguration, {
        clientId: id,
        diagramType: props.languagePlugin.id,
        glspClientProvider: async () => client,
        sourceUri: props.tab.fileUri.toString(),
        editMode: props.languagePlugin.isGenerated ? "layoutable" : EditMode.EDITABLE
    });

    const currentActionDispatcher = container.get<IActionDispatcher>(TYPES.IActionDispatcher);
    actionDispatcher.value = currentActionDispatcher;

    const diagramLoader = container.get(DiagramLoader);
    await diagramLoader.load({ initializeParameters: { applicationId: "mdeo" } });
});
</script>
