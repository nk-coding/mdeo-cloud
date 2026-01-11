<template>
    <div class="flex items-center justify-between gap-2 p-2 rounded-md hover:bg-accent cursor-pointer" @click="toggleExpanded">
        <div class="flex items-center gap-2 flex-1 min-w-0">
            <component :is="expanded ? ChevronDown : ChevronRight" class="w-4 h-4 flex-shrink-0" />
            <Puzzle class="w-4 h-4 flex-shrink-0" />
            <span class="truncate">{{ plugin.name }}</span>
        </div>
    </div>

    <div v-if="expanded" class="pl-8 pb-2 space-y-2 text-sm text-muted-foreground">
        <div class="flex flex-col gap-1">
            <span class="font-medium text-foreground">Description</span>
            <span>{{ plugin.description || "No description available" }}</span>
        </div>
        <div class="flex flex-col gap-1">
            <span class="font-medium text-foreground">URL</span>
            <span class="break-all">{{ plugin.url }}</span>
        </div>
        <div class="pt-2">
            <Button variant="destructive" size="sm" @click.stop="handleDeleteClick">
                <Trash2 class="w-4 h-4 mr-2" />
                Delete Plugin
            </Button>
        </div>
    </div>

    <AlertDialog v-model:open="showDeleteDialog">
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Delete Plugin</AlertDialogTitle>
                <AlertDialogDescription>
                    Are you sure you want to delete "{{ plugin.name }}"? This action cannot be undone.
                </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction @click="confirmDelete">Delete</AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ChevronDown, ChevronRight, Puzzle, Trash2 } from "lucide-vue-next";
import { Button } from "@/components/ui/button";
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
import type { BackendPlugin } from "@/data/api/pluginTypes";

const props = defineProps<{
    plugin: BackendPlugin;
}>();

const emit = defineEmits<{
    delete: [pluginId: string];
}>();

const expanded = ref(false);
const showDeleteDialog = ref(false);

function toggleExpanded() {
    expanded.value = !expanded.value;
}

function handleDeleteClick() {
    showDeleteDialog.value = true;
}

function confirmDelete() {
    emit("delete", props.plugin.id);
    showDeleteDialog.value = false;
}
</script>
