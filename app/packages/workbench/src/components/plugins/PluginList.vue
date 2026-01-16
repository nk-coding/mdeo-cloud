<template>
    <Card class="p-4 gap-0">
        <slot name="header" />

        <SidebarInput v-model="searchText" placeholder="Search plugins..." class="mt-4 bg-transparent" />

        <div class="mt-4 flex-1 min-h-0 overflow-hidden">
            <ScrollArea class="h-full pr-1 **:data-[slot=scroll-area-viewport]:rounded-none">
                <SidebarMenu class="pb-2">
                    <SidebarMenuItem v-for="plugin in filteredPlugins" :key="plugin.id">
                        <SidebarMenuButtonChild
                            class="justify-start gap-3"
                            :is-active="selectedPluginId === plugin.id"
                            @click="$emit('select', plugin.id)"
                        >
                            <Icon :iconNode="plugin.icon" name="PluginIcon" class="w-4 h-4 shrink-0" />
                            <span class="text-sm font-medium truncate">{{ plugin.name }}</span>
                            <slot name="plugin-indicator" :plugin="plugin" />
                        </SidebarMenuButtonChild>
                    </SidebarMenuItem>
                </SidebarMenu>

                <div v-if="isLoading" class="py-6 text-center text-sm text-muted-foreground">Loading plugins...</div>
                <div v-else-if="filteredPlugins.length === 0" class="py-6 text-center text-sm text-muted-foreground">
                    {{ searchText.trim() ? "No plugins match your search." : emptyMessage }}
                </div>
            </ScrollArea>
        </div>
    </Card>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Card } from "@/components/ui/card";
import { SidebarMenu, SidebarMenuItem, SidebarInput } from "@/components/ui/sidebar";
import SidebarMenuButtonChild from "@/components/ui/sidebar/SidebarMenuButtonChild.vue";
import { Icon } from "lucide-vue-next";
import type { Plugin } from "@mdeo/plugin";

const props = withDefaults(
    defineProps<{
        plugins: Plugin[];
        selectedPluginId?: string | null;
        isLoading?: boolean;
        emptyMessage?: string;
    }>(),
    {
        selectedPluginId: null,
        isLoading: false,
        emptyMessage: "No plugins available."
    }
);

defineEmits<{
    select: [pluginId: string];
}>();

const searchText = ref("");

const filteredPlugins = computed(() => {
    if (!searchText.value.trim()) {
        return props.plugins;
    }
    const search = searchText.value.toLowerCase();
    return props.plugins.filter(
        (p) =>
            p.name.toLowerCase().includes(search) ||
            p.description?.toLowerCase().includes(search) ||
            p.url.toLowerCase().includes(search)
    );
});
</script>
