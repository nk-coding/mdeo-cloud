<template>
    <div class="h-full flex flex-col">
        <div class="flex flex-1 min-h-0 gap-6">
            <PluginList
                :plugins="plugins"
                :selected-plugin-id="selectedEntry === 'add' ? null : selectedEntry"
                :is-loading="isLoading"
                empty-message="No plugins installed yet."
                @select="selectPlugin"
            >
                <template #header>
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
                </template>
            </PluginList>

            <PluginDetailsContainer :plugin="selectedPlugin">
                <template #content>
                    <div v-if="selectedPlugin != undefined" class="space-y-6">
                        <PluginDetails :plugin="selectedPlugin">
                            <template #actions>
                                <Button
                                    variant="destructive"
                                    size="sm"
                                    :disabled="deletingPluginId === selectedPlugin.id"
                                    @click="openDeleteDialog"
                                >
                                    <Trash2 class="w-4 h-4 mr-2" />
                                    Delete
                                </Button>
                            </template>
                        </PluginDetails>
                    </div>
                </template>
                <template #empty>
                    <div>
                        <h2 class="text-2xl font-semibold text-foreground">Add plugin</h2>
                        <p class="text-sm text-muted-foreground my-2">
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
                    <p class="text-xs text-muted-foreground mt-2">
                        Plugins have full access to your workbench. Only install sources that you trust.
                    </p>
                </template>
            </PluginDetailsContainer>
        </div>
    </div>
    <AlertDialog v-model:open="showDeleteDialog">
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Delete plugin</AlertDialogTitle>
                <AlertDialogDescription>
                    Removing "{{ selectedPlugin?.name }}" will make it unavailable to all projects. This action cannot
                    be undone.
                </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction :disabled="deletingPluginId != undefined" @click="confirmDelete"
                    >Delete</AlertDialogAction
                >
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { PlusCircle, Trash2 } from "lucide-vue-next";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { SidebarMenu, SidebarMenuItem } from "@/components/ui/sidebar";
import SidebarMenuButtonChild from "@/components/ui/sidebar/SidebarMenuButtonChild.vue";
import PluginList from "@/components/plugins/PluginList.vue";
import PluginDetailsContainer from "@/components/plugins/PluginDetailsContainer.vue";
import type { BackendApi } from "@/data/api/backendApi";
import type { BackendPlugin } from "@/data/api/pluginTypes";
import PluginDetails from "../plugins/PluginDetails.vue";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle
} from "@/components/ui/alert-dialog";

const props = defineProps<{
    backendApi: BackendApi;
}>();

const plugins = ref<BackendPlugin[]>([]);
const newPluginUrl = ref("");
const isLoading = ref(false);
const isAdding = ref(false);
const addError = ref("");
const selectedEntry = ref<"add" | string>("add");
const deletingPluginId = ref<string>();
const hasLoaded = ref(false);

const selectedPlugin = computed(() => {
    if (selectedEntry.value === "add") {
        return null;
    }
    return plugins.value.find((plugin) => plugin.id === selectedEntry.value) ?? null;
});

watch(plugins, (list) => {
    if (!hasLoaded.value) {
        return;
    }
    if (list.length === 0) {
        selectedEntry.value = "add";
    } else if (selectedEntry.value !== "add" && !list.find((p) => p.id === selectedEntry.value)) {
        const firstPlugin = list[0];
        if (firstPlugin) {
            selectedEntry.value = firstPlugin.id;
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
            } else if (
                selectedEntry.value !== "add" &&
                !plugins.value.some((plugin) => plugin.id === selectedEntry.value)
            ) {
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

const showDeleteDialog = ref(false);

function openDeleteDialog() {
    showDeleteDialog.value = true;
}

async function confirmDelete() {
    showDeleteDialog.value = false;
    if (selectedPlugin.value == undefined) {
        return;
    }
    const pluginId = selectedPlugin.value.id;
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
