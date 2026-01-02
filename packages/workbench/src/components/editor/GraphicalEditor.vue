<template>
    <div v-if="editorPlugin != undefined" :id="id"></div>
</template>
<script setup lang="ts">
import "reflect-metadata";
import { createContainer } from "@mdeo/editor-common";
import { computed, inject, onMounted, useId } from "vue";
import type { EditorTab } from "@/data/tab/editorTab";
import { workbenchStateKey } from "../workbench/util";
import { MonacoGLSPClient } from "./glspClient";
import { DiagramLoader } from "@eclipse-glsp/client";

const props = defineProps<{
    tab: EditorTab;
}>();

const { languagePluginByExtension, languageClient } = inject(workbenchStateKey)!;

const id = useId();

const languagePlugin = computed(() => languagePluginByExtension.value.get(props.tab.file.extension)!);

const editorPlugin = computed(() => {
    return languagePlugin.value.editorPlugin;
});

onMounted(async () => {
    const client = new MonacoGLSPClient({
        client: languageClient.value!,
        id: id,
        uri: props.tab.file.id.toString()
    });

    const plugin = editorPlugin.value;
    if (plugin == undefined) {
        return undefined;
    }
    const container = createContainer(plugin, {
        clientId: id,
        diagramType: languagePlugin.value.id,
        glspClientProvider: async () => client,
        sourceUri: props.tab.file.id.toString()
    });

    const diagramLoader = container.get(DiagramLoader)
    await diagramLoader.load()
});
</script>
