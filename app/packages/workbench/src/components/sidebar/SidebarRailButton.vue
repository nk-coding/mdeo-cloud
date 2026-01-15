<template>
    <Tooltip>
        <TooltipTrigger asChild>
            <button
                v-bind="$attrs"
                :class="
                    cn(
                        'flex items-center justify-center gap-2 overflow-hidden rounded-md p-2 text-left text-sm h-10 w-10',
                        'outline-hidden ring-sidebar-ring transition-[width,height,padding] focus-visible:ring-2',
                        'hover:bg-accent hover:text-accent-foreground active:bg-accent active:text-accent-foreground',
                        'data-[active=true]:bg-accent data-[active=true]:font-medium data-[active=true]:text-accent-foreground',
                        'hover:bg-accent hover:text-accent-foreground',
                        '[&>span:last-child]:truncate [&>svg]:size-5 [&>svg]:shrink-0'
                    )
                "
                :data-active="active && !sidebarCollapsed"
            >
                <slot />
            </button>
        </TooltipTrigger>
        <TooltipContent :side="'right'" :align="'center'">
            {{ tooltip }}
        </TooltipContent>
    </Tooltip>
</template>

<script setup lang="ts">
import { cn } from "../../lib/utils";
import { Tooltip, TooltipContent, TooltipTrigger } from "../ui/tooltip";
import { inject, ref } from "vue";
import { workbenchStateKey } from "../workbench/util";

defineProps<{
    tooltip: string;
    active?: boolean;
}>();

const injectedState = inject(workbenchStateKey, null);
const sidebarCollapsed = injectedState?.sidebarCollapsed ?? ref(true);
</script>
