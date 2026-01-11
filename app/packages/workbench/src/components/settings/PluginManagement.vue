<template>
    <div class="h-full flex flex-col">
        <div class="flex flex-1 min-h-0 gap-6">
            <div class="w-72 shrink-0 rounded-2xl border border-border/40 bg-muted/30 p-4 flex flex-col min-h-0">
                <SidebarMenu>
                    <SidebarMenuItem>
                        <SidebarMenuButtonChild
                            class="gap-2 justify-start"
                            :is-active="selectedEntry === 'add'"
                            @click="selectAddEntry"
                        >
                            <PlusCircle class="w-4 h-4" />
                            <span class="text-sm font-medium">Add plugin</span>
                        </SidebarMenuButtonChild>
                    </SidebarMenuItem>
                </SidebarMenu>

                <SidebarInput
                    v-model="searchText"
                    placeholder="Search plugins..."
                    class="mt-4 bg-transparent"
                />

                <div class="mt-4 flex-1 min-h-0 overflow-hidden">
                    <ScrollArea class="h-full pr-1 [&_[data-slot=scroll-area-viewport]]:rounded-none">
                        <SidebarMenu class="pb-2">
                            <SidebarMenuItem v-for="plugin in filteredPlugins" :key="plugin.id">
                                <SidebarMenuButtonChild
                                    class="justify-start gap-3"
                                    :is-active="selectedEntry === plugin.id"
                                    @click="selectPlugin(plugin.id)"
                                >
                                    <Puzzle class="w-4 h-4 shrink-0" />
                                    <span class="text-sm font-medium truncate">{{ plugin.name }}</span>
                                </SidebarMenuButtonChild>
                            </SidebarMenuItem>
                        </SidebarMenu>

                        <div v-if="isLoading" class="py-6 text-center text-sm text-muted-foreground">
                            Loading plugins...
                        </div>
                        <div v-else-if="filteredPlugins.length === 0" class="py-6 text-center text-sm text-muted-foreground">
                            {{ searchText.trim() ? "No plugins match your search." : "No plugins installed yet." }}
                        </div>
                    </ScrollArea>
                </div>
            </div>

            <div class="flex-1 min-h-0 rounded-2xl border border-border/40 bg-muted/30 overflow-hidden">
                <ScrollArea class="h-full [&_[data-slot=scroll-area-viewport]]:rounded-none">
                    <div class="p-6">
                        <section v-if="selectedEntry === 'add'" class="space-y-6">
                            <div>
                                <h2 class="text-2xl font-semibold text-foreground">Add plugin</h2>
                                <p class="text-sm text-muted-foreground mt-1">
                                    Install a new integration by providing the plugin entry URL.
                                </p>
                            </div>
                            <div class="flex flex-col gap-3 sm:flex-row">
                                <Input
                                    v-model="newPluginUrl"
                                    placeholder="https://example.com/plugin.json"
                                    class="flex-1"
                                    @keydown.enter="handleAddPlugin"
                                />
                                <Button class="sm:w-auto" :disabled="!newPluginUrl.trim() || isAdding" @click="handleAddPlugin">
                                    <PlusCircle class="w-4 h-4 mr-2" />
                                    Add plugin
                                </Button>
                            </div>
                            <p v-if="addError" class="text-sm text-destructive">{{ addError }}</p>
                            <p class="text-xs text-muted-foreground">
                                Plugins have full access to your workbench. Only install sources that you trust.
                            </p>
                        </section>

                        <section v-else-if="selectedPlugin" class="space-y-6">
                            <PluginDetails
                                :plugin="selectedPlugin"
                                :deleting="deletingPluginId === selectedPlugin.id"
                                @delete="handleDeletePlugin"
                            />
                        </section>

                        <section v-else class="py-10 text-center text-sm text-muted-foreground">
                            Select a plugin from the sidebar to view its details.
                        </section>
                    </div>
                </ScrollArea>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { PlusCircle, Puzzle } from "lucide-vue-next";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import ScrollArea from "@/components/ui/scroll-area/ScrollArea.vue";
import PluginDetails from "./PluginDetails.vue";
import SidebarMenu from "@/components/ui/sidebar/SidebarMenu.vue";
import SidebarMenuItem from "@/components/ui/sidebar/SidebarMenuItem.vue";
import SidebarMenuButtonChild from "@/components/ui/sidebar/SidebarMenuButtonChild.vue";
import SidebarInput from "@/components/ui/sidebar/SidebarInput.vue";
import type { BackendApi } from "@/data/api/backendApi";
import type { BackendPlugin } from "@/data/api/pluginTypes";

const props = defineProps<{
    backendApi: BackendApi;
}>();

const plugins = ref<BackendPlugin[]>([]);
const searchText = ref("");
const newPluginUrl = ref("");
const isLoading = ref(false);
const isAdding = ref(false);
const addError = ref("");
const selectedEntry = ref<"add" | string>("add");
const deletingPluginId = ref<string>();
const hasLoaded = ref(false);

const filteredPlugins = computed(() => {
    if (!searchText.value.trim()) {
        return plugins.value;
    }
    const search = searchText.value.toLowerCase();
    return plugins.value.filter(
        (p) =>
            p.name.toLowerCase().includes(search) ||
            p.description?.toLowerCase().includes(search) ||
            p.url.toLowerCase().includes(search)
    );
});

const selectedPlugin = computed(() => {
    if (selectedEntry.value === "add") {
        return null;
    }
    return plugins.value.find((plugin) => plugin.id === selectedEntry.value) ?? null;
});

watch(filteredPlugins, (list) => {
    if (!hasLoaded.value) {
        return;
    }
    if (list.length === 0) {
        selectedEntry.value = "add";
        return;
    }
    if (selectedEntry.value === "add") {
        return;
    }
    if (!list.some((plugin) => plugin.id === selectedEntry.value)) {
        const firstVisible = list[0];
        if (firstVisible) {
            selectedEntry.value = firstVisible.id;
        }
    }
});

function selectPlugin(pluginId: string) {
    selectedEntry.value = pluginId;
}

function selectAddEntry() {
    selectedEntry.value = "add";
}

async function loadPlugins(preferredSelection?: string) {
    isLoading.value = true;
    try {
        const result = await props.backendApi.getPlugins();
        if (result.success) {
            plugins.value = result.value;
            hasLoaded.value = true;
            if (preferredSelection) {
                selectedEntry.value = preferredSelection;
            } else if (plugins.value.length === 0) {
                selectedEntry.value = "add";
            } else if (selectedEntry.value !== "add" && !plugins.value.some((plugin) => plugin.id === selectedEntry.value)) {
                const firstPlugin = plugins.value[0];
                if (firstPlugin) {
                    selectedEntry.value = firstPlugin.id;
                }
            }
        }
    } finally {
        isLoading.value = false;
    }
}

async function handleAddPlugin() {
    const url = newPluginUrl.value.trim();
    if (!url) return;

    isAdding.value = true;
    addError.value = "";

    try {
        const result = await props.backendApi.createPlugin(url);
        if (result.success) {
            newPluginUrl.value = "";
            await loadPlugins(result.value);
        } else {
            addError.value = `Failed to add plugin: ${result.error.message}`;
        }
    } catch (e) {
        addError.value = `Failed to add plugin: ${e instanceof Error ? e.message : "Unknown error"}`;
    } finally {
        isAdding.value = false;
    }
}

async function handleDeletePlugin(pluginId: string) {
    deletingPluginId.value = pluginId;
    const result = await props.backendApi.deletePlugin(pluginId);
    if (result.success) {
        selectedEntry.value = "add";
        await loadPlugins();
    }
    deletingPluginId.value = undefined;
}

onMounted(async () => {
    await loadPlugins();
});
</script>
