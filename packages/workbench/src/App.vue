<template>
    <div class="h-screen w-screen flex">
        <SidebarRail />
        <div ref="workbench" class="flex-1">
            <ResizablePanelGroup direction="horizontal">
                <SplitterPanel
                    ref="sidebarPanel"
                    :min-size="minSidebarWidth"
                    :max-size="80"
                    :default-size="sidebarDefaultWidth"
                    :collapsed-size="0"
                    collapsible
                    @resize="onSidebarResize"
                    @collapse="workbenchState.sidebarCollapsed.value = true"
                    @expand="workbenchState.sidebarCollapsed.value = false"
                >
                    <Sidebar />
                </SplitterPanel>
                <ResizableHandle />
                <SplitterPanel>
                    <div class="flex flex-col h-full">
                        <Tabs />
                        <Editor />
                    </div>
                </SplitterPanel>
            </ResizablePanelGroup>
        </div>
    </div>
</template>
<script setup lang="ts">
import { computed, provide, ref, shallowRef, useTemplateRef, watch } from "vue";
import Tabs from "./components/tabs/Tabs.vue";
import { BrowserFileSystem } from "./data/filesystem/browserFileSystem";
import { WorkbenchState, workbenchStateKey } from "./data/workbenchState";
import { ResizablePanelGroup, ResizableHandle } from "./components/ui/resizable";
import Sidebar from "./components/sidebar/Sidebar.vue";
import SidebarRail from "./components/sidebar/SidebarRail.vue";
import Editor from "./components/editor/Editor.vue";
import { useResizeObserver } from "@vueuse/core";
import { SplitterPanel } from "reka-ui";

const workbenchState = shallowRef(
    new WorkbenchState(new BrowserFileSystem(), [
        {
            id: "metamodel",
            name: "Metamodel",
            extension: ".mm"
        }
    ])
);
provide(workbenchStateKey, workbenchState);

const workbenchWidth = ref<number>();
const absoluteMinSidebarWidth = 150;
const minSidebarWidth = ref(10);
const absoluteSidebarWidth = ref(300);

const workbench = useTemplateRef("workbench");
const sidebarPanel = useTemplateRef("sidebarPanel");

const sidebarDefaultWidth = computed(() => {
    if (workbenchWidth.value != undefined) {
        return (absoluteSidebarWidth.value / workbenchWidth.value) * 100;
    }
    return undefined;
});

useResizeObserver(workbench, (entries) => {
    const entry = entries[0];
    if (entry != undefined) {
        workbenchWidth.value = entry.contentRect.width;
        minSidebarWidth.value = Math.max((absoluteMinSidebarWidth / entry.contentRect.width) * 100, 0.1);
    }
});

function onSidebarResize(size: number) {
    if (workbenchWidth.value != undefined) {
        absoluteSidebarWidth.value = (size / 100) * workbenchWidth.value;
    }
}

const sidebarCollapsed = computed(() => workbenchState.value.sidebarCollapsed.value);

watch(
    sidebarCollapsed,
    (newValue) => {
        if (newValue) {
            sidebarPanel.value?.collapse();
        } else {
            sidebarPanel.value?.expand();
        }
    },
    { immediate: true }
);
</script>
<style>
@supports selector(::-webkit-scrollbar) {
    *::-webkit-scrollbar {
        width: 4px;
        height: 4px;
    }

    *::-webkit-scrollbar-thumb {
        background: transparent;
    }

    *:hover::-webkit-scrollbar-thumb,
    *:focus-within::-webkit-scrollbar-thumb {
        background: var(--muted);
    }
}
</style>
