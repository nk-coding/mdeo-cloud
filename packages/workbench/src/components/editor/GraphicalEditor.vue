<template>
    <div class="sprotty-wrapper w-full h-full relative">
        <div v-if="editorPlugin != undefined" :id="id"></div>
    </div>
</template>
<script setup lang="ts">
import "reflect-metadata";
import { createContainer } from "@mdeo/editor-common";
import { computed, inject, onMounted, useId } from "vue";
import type { EditorTab } from "@/data/tab/editorTab";
import { workbenchStateKey } from "../workbench/util";
import { MonacoGLSPClient } from "./glspClient";
import { DiagramLoader } from "@eclipse-glsp/client";
import { editorContextKey } from "@/lib/editorPlugin";

const props = defineProps<{
    tab: EditorTab;
}>();

const { languagePluginByExtension, languageClient } = inject(workbenchStateKey)!;
const editorContext = inject(editorContextKey)!;

const id = useId();

const languagePlugin = computed(() => languagePluginByExtension.value.get(props.tab.file.extension)!);

const editorPlugin = computed(() => {
    return languagePlugin.value.editorPlugin;
});

onMounted(async () => {
    const client = new MonacoGLSPClient({
        client: languageClient.value!,
        id: id,
    });

    const plugin = editorPlugin.value;
    if (plugin == undefined) {
        return undefined;
    }
    const container = createContainer(editorContext, plugin, {
        clientId: id,
        diagramType: languagePlugin.value.id,
        glspClientProvider: async () => client,
        sourceUri: props.tab.file.id.toString()
    });

    const diagramLoader = container.get(DiagramLoader);
    await diagramLoader.load({ initializeParameters: { applicationId: "mdeo" } });
});
</script>