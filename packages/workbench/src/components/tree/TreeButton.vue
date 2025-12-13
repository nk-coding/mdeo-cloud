<template>
    <Primitive
        data-slot="tree-button"
        data-tree="button"
        :data-active="isActive"
        :class="cn(buttonModes({ mode }), props.class)"
        :as="mode === 'edit' ? 'div' : 'button'"
        :as-child="asChild"
        :draggable="dragAndDropConfig?.enabled && mode !== 'edit'"
        @dragstart="handleDragStart"
        @dragend="handleDragEnd"
        v-bind="$attrs"
    >
        <slot />
    </Primitive>
</template>

<script setup lang="ts">
import { computed, inject, type HTMLAttributes } from "vue";
import type { PrimitiveProps } from "reka-ui";
import { Primitive } from "reka-ui";
import { cn } from "@/lib/utils";
import { activeItemKey, dragAndDropKey, type TreeItem } from "./util";
import { cva } from "class-variance-authority";

export interface TreeButtonProps extends PrimitiveProps {
    class?: HTMLAttributes["class"];
    data: TreeItem;
    mode?: "default" | "edit";
}

defineOptions({
    inheritAttrs: false
});

const props = withDefaults(defineProps<TreeButtonProps>(), {
    mode: "default"
});

const activeItem = inject(activeItemKey);
const dragAndDropConfig = inject(dragAndDropKey);

const isActive = computed(() => {
    return activeItem?.value?.id === props.data.id;
});

function handleDragStart(event: DragEvent) {
    if (!dragAndDropConfig?.value.enabled || props.mode === "edit") {
        return;
    }

    event.dataTransfer?.setData("application/json", JSON.stringify({ id: props.data.id }));
    event.dataTransfer!.effectAllowed = "move";
    dragAndDropConfig.value.callbacks?.onDragStart?.(props.data, event);
}

function handleDragEnd(event: DragEvent) {
    if (!dragAndDropConfig?.value.enabled || props.mode === "edit") {
        return;
    }

    dragAndDropConfig.value.callbacks?.onDragEnd?.(props.data, event);
}

const buttonModes = cva(
    [
        "flex w-full items-center gap-2 overflow-hidden rounded-md p-2 tet-left text-sm outline-none ring-primary h-8 cursor-pointer",
        "[&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0",
        "transition-[width,height,padding,color,box-shadow]"
    ],
    {
        variants: {
            mode: {
                default: [
                    "hover:bg-surface-container-high",
                    "group-focus/tree:data-[active=true]:bg-primary group-focus/tree:data-[active=true]:text-primary-foreground focus:data-[active=true]:bg-primary focus:data-[active=true]:text-primary-foreground",
                    "focus-within:data-[active=false]:[&:not(:active)]:bg-primary-container focus-within:data-[active=false]:[&:not(:active)]:text-primary-container-foreground",
                    "data-[state=open]:bg-primary-container data-[state=open]:text-primary-container-foreground",
                    "data-[active=true]:bg-accent",
                    "[&_*]:pointer-events-none"
                ],
                edit: [
                    "border",
                    "border-ring ring-ring/50 ring-[3px]",
                    "[&:has([aria-invalid='true'])]:ring-destructive/20 dark:[&:has([aria-invalid='true'])]:ring-destructive/40 [&:has([aria-invalid='true'])]:border-destructive"
                ]
            }
        }
    }
);
</script>
