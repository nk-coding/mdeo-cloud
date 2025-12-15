<template>
    <div class="h-screen w-screen flex">
        <SidebarRail />
        <div ref="workbench" class="flex-1 min-w-0">
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
                    <div class="flex flex-col h-full w-full">
                        <Tabs />
                        <Editor />
                    </div>
                </SplitterPanel>
            </ResizablePanelGroup>
        </div>
    </div>
</template>
<script setup lang="ts">
import { computed, provide, ref, useTemplateRef, watch } from "vue";
import Tabs from "../tabs/Tabs.vue";
import { ResizablePanelGroup, ResizableHandle } from "../ui/resizable";
import Sidebar from "../sidebar/Sidebar.vue";
import SidebarRail from "../sidebar/SidebarRail.vue";
import Editor from "../editor/Editor.vue";
import { useResizeObserver } from "@vueuse/core";
import { SplitterPanel } from "reka-ui";
import { workbenchStateKey } from "./util";
import type { WorkbenchState } from "@/data/workbenchState";

const props = defineProps<{
    workbenchState: WorkbenchState;
}>();

provide(workbenchStateKey, props.workbenchState);

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

const sidebarCollapsed = computed(() => props.workbenchState.sidebarCollapsed.value);

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
