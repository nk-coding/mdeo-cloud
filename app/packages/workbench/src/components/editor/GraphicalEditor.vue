<template>
    <div ref="sprottyWrapper" class="sprotty-wrapper w-full h-full relative ml-1.5">
        <div v-if="editorPlugin != undefined" :id="id"></div>
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
import { ResetCanvasBoundsAction } from "@mdeo/editor-protocol";
import type { IActionDispatcher } from "@eclipse-glsp/sprotty";
import { TYPES } from "@eclipse-glsp/sprotty";

const props = defineProps<{
    tab: EditorTab;
}>();

const { languagePluginByExtension, languageClient } = inject(workbenchStateKey)!;
const editorContext = inject(editorContextKey)!;

const id = useId();
const sprottyWrapper = ref<HTMLElement | null>(null);
const actionDispatcher = shallowRef<IActionDispatcher>();

const languagePlugin = computed(() => languagePluginByExtension.value.get(props.tab.file.extension));

const editorPlugin = computed(() => {
    return languagePlugin.value?.editorPlugin;
});

useResizeObserver(sprottyWrapper, () => {
    actionDispatcher.value?.dispatch({ kind: ResetCanvasBoundsAction.KIND } satisfies ResetCanvasBoundsAction);
});

onMounted(async () => {
    if (languagePlugin.value == undefined) {
        return;
    }

    const client = new MonacoGLSPClient({
        client: languageClient.value!,
        id: id
    });

    const plugin = editorPlugin.value;
    if (plugin == undefined) {
        return undefined;
    }
    const container = createContainer(editorContext, plugin.containerConfiguration, {
        clientId: id,
        diagramType: languagePlugin.value.id,
        glspClientProvider: async () => client,
        sourceUri: props.tab.file.id.toString()
    });

    const currentActionDispatcher = container.get<IActionDispatcher>(TYPES.IActionDispatcher);
    actionDispatcher.value = currentActionDispatcher;

    const diagramLoader = container.get(DiagramLoader);
    await diagramLoader.load({ initializeParameters: { applicationId: "mdeo" } });
});
</script>
