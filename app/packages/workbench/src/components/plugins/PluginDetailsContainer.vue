<template>
    <Card class="p-0 gap-0 flex-1 min-w-0">
        <ScrollArea class="h-full **:data-[slot=scroll-area-viewport]:rounded-none">
            <div class="p-6">
                <div v-if="plugin" class="space-y-6">
                    <slot name="content"></slot>
                </div>
                <div v-else class="py-10 text-center text-sm text-muted-foreground">
                    <slot name="empty"> Select a plugin to view its details. </slot>
                </div>
            </div>
        </ScrollArea>
    </Card>
</template>

<script setup lang="ts">
import { ScrollArea } from "@/components/ui/scroll-area";
import { Card } from "@/components/ui/card";
import type { Plugin } from "@mdeo/plugin";

defineProps<{
    plugin?: Plugin;
}>();
</script>
