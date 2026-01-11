<template>
    <template v-if="tabs.length > 0">
        <ScrollArea orientation="horizontal" class="w-full min-w-0" @wheel="handleWheel">
            <TabsRoot v-model="activeTabPath">
                <TabsList
                    ref="tabsRef"
                    class="text-muted-foreground inline-flex w-fit items-center justify-start rounded-lg gap-1 m-1.5"
                >
                    <FileTab
                        v-for="tab in tabs"
                        :key="tab.file.id.toString()"
                        :tab="tab"
                        :is-active="activeTab === tab"
                        @close="closeTab"
                    />
                </TabsList>
            </TabsRoot>
        </ScrollArea>
        <Separator />
    </template>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, useTemplateRef, watch } from "vue";
import { TabsRoot, TabsList } from "reka-ui";
import FileTab from "./FileTab.vue";
import type { EditorTab } from "@/data/tab/editorTab";
import { Separator } from "../ui/separator";
import ScrollArea from "../ui/scroll-area/ScrollArea.vue";
import { workbenchStateKey } from "../workbench/util";
import { watchArray } from "@vueuse/core";

const { tabs, activeTab } = inject(workbenchStateKey)!;

const tabsRef = useTemplateRef("tabsRef");

const activeTabPath = computed({
    get: () => activeTab.value?.file?.id?.toString(),
    set: (path: string) => {
        const tab = tabs.value.find((t) => t.file.id.toString() === path);
        if (tab != undefined) {
            activeTab.value = tab;
        }
    }
});

function closeTab(tab: EditorTab) {
    const currentTabs = tabs.value;
    const index = currentTabs.findIndex((t) => t === tab);

    if (index === -1) {
        return;
    }

    tabs.value = currentTabs.filter((t) => t !== tab);

    if (activeTab.value === tab) {
        const remainingTabs = tabs.value;
        if (remainingTabs.length > 0) {
            const newIndex = Math.min(index, remainingTabs.length - 1);
            activeTab.value = remainingTabs[newIndex];
        } else {
            activeTab.value = undefined;
        }
    }
}

function handleWheel(event: WheelEvent) {
    if (event.deltaY !== 0) {
        const target = event.currentTarget as HTMLElement;
        const scrollViewport = target.querySelector('[data-slot="scroll-area-viewport"]') as HTMLElement;

        if (scrollViewport) {
            event.preventDefault();
            scrollViewport.scrollLeft += event.deltaY;
        }
    }
}

watch(activeTab, (newTab) => {
    if (newTab != undefined) {
        nextTick(() => {
            const tabsElement = tabsRef.value!.$el as HTMLElement | undefined;
            const activeTabElement = tabsElement?.querySelector('[data-state="active"]') as HTMLElement | undefined;
            if (activeTabElement != undefined) {
                activeTabElement.scrollIntoView({ block: "nearest", behavior: "instant" });
            }
        });
    }
});

watchArray(
    tabs,
    (newTabs, oldTabs) => {
        const oldTemporaryTabs = oldTabs.filter((tab) => tab.temporary);
        const newTemporaryTabs = newTabs.filter((tab) => tab.temporary);
        if (newTemporaryTabs.length > oldTemporaryTabs.length) {
            for (const tab of oldTemporaryTabs) {
                closeTab(tab);
            }
        }
    },
    { deep: true }
);
</script>
