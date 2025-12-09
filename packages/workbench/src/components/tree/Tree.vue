<template>
    <div
        ref="treeRef"
        tabindex="0"
        :class="cn('flex min-h-0 flex-1 flex-col gap-2 overflow-x-auto w-full group/tree', props.class)"
        @keydown="handleKeydown"
    >
        <slot />
    </div>
</template>

<script setup lang="ts">
import { computed, provide, ref, useTemplateRef, type HTMLAttributes } from "vue";
import { cn } from "@/lib/utils";
import { activeItemKey } from "./util";

const props = defineProps<{
    class?: HTMLAttributes["class"];
    activeElement?: any;
}>();

const treeRef = useTemplateRef("treeRef");
const activeElement = computed(() => props.activeElement);

provide(activeItemKey, activeElement);

const handleKeydown = (event: KeyboardEvent) => {
    if (event.key !== "ArrowUp" && event.key !== "ArrowDown") {
        return;
    }

    event.preventDefault();

    if (!treeRef.value) {
        return;
    }

    const treeButtons = Array.from(treeRef.value.querySelectorAll('[data-tree="button"]')) as HTMLElement[];

    if (treeButtons.length === 0) {
        return;
    }
    let currentButton = treeButtons.find((button) => button === document.activeElement) as HTMLElement;

    if (!currentButton) {
        currentButton = treeRef.value.querySelector('[data-active="true"][data-tree="button"]') as HTMLElement;
    }

    if (!currentButton) {
        treeButtons[0]?.focus();
        return;
    }

    const currentIndex = treeButtons.indexOf(currentButton);

    if (currentIndex === -1) {
        return;
    }

    let nextIndex: number;

    if (event.key === "ArrowDown") {
        nextIndex = currentIndex + 1;
        if (nextIndex >= treeButtons.length) {
            nextIndex = 0;
        }
    } else {
        nextIndex = currentIndex - 1;
        if (nextIndex < 0) {
            nextIndex = treeButtons.length - 1;
        }
    }

    const nextButton = treeButtons[nextIndex];
    if (nextButton) {
        nextButton.focus();
    }
};
</script>
