<template>
    <Primitive
        data-slot="tree-button"
        data-tree="button"
        :data-active="isActive"
        :class="
            cn(
                'flex w-full items-center gap-2 overflow-hidden rounded-md p-2 text-left text-sm outline-hidden ring-primary h-8',
                'transition-[width,height,padding]',
                'hover:bg-surface-container',
                'group-focus-within/tree:data-[active=true]:bg-primary group-focus-within/tree:data-[active=true]:text-primary-foreground',
                'focus-within:data-[active=false]:[&:not(:active)]:bg-primary-container focus-within:data-[active=false]:[&:not(:active)]:text-primary-container-foreground',
                'data-[active=true]:bg-surface-container-highest',
                '[&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0',
                'outline-none cursor-pointer',
                props.class
            )
        "
        :as="as"
        :as-child="asChild"
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
import { activeItemKey, type TreeItem } from "./util";

export interface TreeButtonProps extends PrimitiveProps {
    class?: HTMLAttributes["class"];
    data: TreeItem;
}

defineOptions({
    inheritAttrs: false
});

const props = withDefaults(defineProps<TreeButtonProps>(), {
    as: "button"
});

const activeItem = inject(activeItemKey);

const isActive = computed(() => {
    return activeItem?.value?.key === props.data.key;
});
</script>
