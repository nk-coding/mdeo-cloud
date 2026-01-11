<template>
    <Primitive
        data-slot="tree-button"
        data-tree="button"
        :data-active="isActive"
        :class="cn(buttonModes({ mode }), props.class)"
        :as="mode === 'edit' ? 'div' : 'button'"
        :as-child="asChild"
        :draggable="treeContext?.dragAndDrop.value.enabled && mode !== 'edit'"
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
import { treeContextKey, type TreeItem } from "./util";
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

const treeContext = inject(treeContextKey);

const isActive = computed(() => {
    return treeContext?.activeItem?.value?.id === props.data.id;
});

function handleDragStart(event: DragEvent) {
    if (!treeContext?.dragAndDrop.value.enabled || props.mode === "edit") {
        return;
    }

    event.dataTransfer?.setData("application/json", JSON.stringify({ id: props.data.id }));
    event.dataTransfer!.effectAllowed = "move";
    treeContext.dragAndDrop.value.callbacks?.onDragStart?.(props.data, event);
}

function handleDragEnd(event: DragEvent) {
    if (!treeContext?.dragAndDrop.value.enabled || props.mode === "edit") {
        return;
    }

    treeContext.dragAndDrop.value.callbacks?.onDragEnd?.(props.data, event);
}

const buttonModes = cva(
    [
        "flex w-full items-center gap-2 overflow-hidden rounded-md p-2 tet-left text-sm outline-none ring-sidebar-primary h-8",
        "[&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0",
        "transition-[width,height,padding,box-shadow]",
        "group/tree-button"
    ],
    {
        variants: {
            mode: {
                default: [
                    "hover:bg-accent/75",
                    "group-focus-within/tree:data-[active=true]:bg-sidebar-primary group-focus-within/tree:data-[active=true]:text-sidebar-primary-foreground",
                    "focus-within:data-[active=false]:[&:not(:active)]:bg-sidebar-primary/70 focus-within:data-[active=false]:[&:not(:active)]:text-sidebar-primary-foreground",
                    "data-[state=open]:data-[slot=context-menu-trigger]:bg-sidebar-primary/70 data-[state=open]:data-[slot=context-menu-trigger]:text-sidebar-primary-foreground",
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
