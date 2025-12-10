<template>
    <TreeButton v-if="!items" v-bind="$attrs" :data="data" :mode="mode">
        <slot name="content" />
    </TreeButton>

    <li v-else data-slot="tree-item" data-tree="item" :class="cn('group/tree-item relative')">
        <Collapsible class="group/collapsible [&[data-state=open]>button>svg:first-child]:rotate-90">
            <CollapsibleTrigger as-child>
                <TreeButton v-bind="$attrs" :data="data" :mode="mode">
                    <ChevronRight class="transition-transform" />
                    <slot name="content" />
                </TreeButton>
            </CollapsibleTrigger>
            <CollapsibleContent v-if="items.length > 0">
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
                    <template v-for="(item, index) in items" :key="index">
                        <slot name="item" :item="item" />
                    </template>
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

defineProps<
    {
        data: T;
        items?: T[];
    } & TreeButtonProps
>();
</script>
