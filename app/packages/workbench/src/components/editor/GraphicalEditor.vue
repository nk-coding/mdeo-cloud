<template>
    <div
        ref="sprottyWrapper"
        class="sprotty-wrapper w-full h-full relative ml-1.5"
        :class="graphicalEditorPlugin?.stylesCls"
        @wheel.capture="handleWheel"
    >
        <div v-if="graphicalEditorPlugin != undefined" :id="id"></div>
    </div>
</template>
<script setup lang="ts">
import "reflect-metadata";
import { createContainer } from "@mdeo/editor-common";
import { computed, inject, onMounted, onUnmounted, ref, shallowRef, useId, watch } from "vue";
import type { EditorTab } from "@/data/tab/editorTab";
import { workbenchStateKey } from "../workbench/util";
import { DiagramLoader } from "@eclipse-glsp/client";
import { editorContextKey } from "@/lib/editorPlugin";
import { useResizeObserver, useStyleTag } from "@vueuse/core";
import type { ResetCanvasBoundsAction } from "@mdeo/protocol-common";
import type { IActionDispatcher } from "@eclipse-glsp/sprotty";
import { EditMode, TYPES } from "@eclipse-glsp/sprotty";
import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";
import type { UpdateClientOperation } from "@mdeo/language-shared";

const props = defineProps<{
    tab: EditorTab;
    languagePlugin: ResolvedWorkbenchLanguagePlugin;
    editable: boolean;
    isActive: boolean;
    captureWheelscroll?: boolean;
}>();

const { glspClient } = inject(workbenchStateKey)!;
const editorContext = inject(editorContextKey)!;

const id = useId();
const sprottyWrapper = ref<HTMLElement | null>(null);
const actionDispatcher = shallowRef<IActionDispatcher>();

const diagramLoaded = ref(false);

watch(
    () => props.isActive,
    (active) => {
        if (active && diagramLoaded.value) {
            const action: UpdateClientOperation = {
                kind: "updateClientOperation",
                isOperation: true
            };
            actionDispatcher.value?.dispatch(action);
        }
    }
);

const graphicalEditorPlugin = computed(() => {
    return props.languagePlugin.graphicalEditorPlugin;
});

useResizeObserver(sprottyWrapper, () => {
    actionDispatcher.value?.dispatch({ kind: "resetCanvasBoundsAction" } satisfies ResetCanvasBoundsAction);
});

/**
 * Handles wheel events on the graphical editor.
 * If capture is enabled, stops propagation only when focus is outside the editor.
 */
function handleWheel(event: WheelEvent): void {
    if (!props.captureWheelscroll) {
        return;
    }

    const focusedElement = document.activeElement;
    if (sprottyWrapper.value && focusedElement != null && !sprottyWrapper.value.contains(focusedElement)) {
        event.stopPropagation();
    }
}

onMounted(async () => {
    if (props.languagePlugin.graphicalEditorPlugin == undefined) {
        return;
    }

    const plugin = graphicalEditorPlugin.value;
    if (plugin == undefined) {
        return undefined;
    }
    const container = createContainer(editorContext, plugin.containerConfiguration, {
        clientId: id,
        diagramType: props.languagePlugin.id,
        glspClientProvider: async () => glspClient.value!,
        sourceUri: props.tab.fileUri.toString(),
        editMode: props.editable ? EditMode.EDITABLE : "layoutable"
    });

    const currentActionDispatcher = container.get<IActionDispatcher>(TYPES.IActionDispatcher);
    actionDispatcher.value = currentActionDispatcher;

    const diagramLoader = container.get(DiagramLoader);
    await diagramLoader.load({ initializeParameters: { applicationId: "mdeo" } });
    diagramLoaded.value = true;
});

onUnmounted(() => {
    document.getElementById(`${id}_hidden`)?.remove();
});

useStyleTag(computed(() => `#${id}_hidden { display: ${props.isActive ? "block" : "none"}; }`));
</script>
