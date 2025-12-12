<template>
    <TreeButton v-if="!isFolder" v-bind="$attrs" :data="data" :mode="mode">
        <slot name="content" />
    </TreeButton>

    <li v-else data-slot="tree-item" data-tree="item" :class="cn('group/tree-item relative')">
        <Collapsible
            v-model:open="isOpen"
            class="group/collapsible [&[data-state=open]>button>svg:first-child]:rotate-90"
        >
            <CollapsibleTrigger as-child>
                <TreeButton v-bind="$attrs" :data="data" :mode="mode">
                    <ChevronRight class="transition-transform" />
                    <slot name="content" />
                </TreeButton>
            </CollapsibleTrigger>
            <CollapsibleContent v-if="hasChildren">
                <ul
                    data-slot="tree-sub"
                    data-tree="sub"
                    :class="
                        cn(
                            'border-sidebar-border ml-3.5 mr-px flex min-w-0 translate-x-px flex-col gap-1 border-l pl-2.5 py-0.5',
                            'group-data-[collapsible=icon]:hidden'
                        )
                    "
                    @click.stop
                >
                    <slot name="items" />
                </ul>
            </CollapsibleContent>
        </Collapsible>
    </li>
</template>

<script setup lang="ts" generic="T extends TreeItem">
import { ChevronRight } from "lucide-vue-next";
import { cn } from "@/lib/utils";

import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import TreeButton, { type TreeButtonProps } from "./TreeButton.vue";
import type { TreeItem } from "./util";
import { computed, ref } from "vue";

const props = defineProps<
    {
        data: T;
        isFolder: boolean;
        hasChildren: boolean;
        forceOpen?: boolean;
    } & TreeButtonProps
>();

const _isOpen = ref(false);

const isOpen = computed({
    get() {
        return props.forceOpen || _isOpen.value;
    },
    set(value: boolean) {
        if (props.forceOpen != true) {
            _isOpen.value = value;
        }
    }
});
</script>
