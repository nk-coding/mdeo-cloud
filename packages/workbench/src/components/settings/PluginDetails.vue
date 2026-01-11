<template>
    <div class="space-y-6">
        <div class="flex items-start justify-between gap-4">
            <div class="space-y-3">
                <h2 class="text-2xl font-semibold text-foreground">
                    {{ plugin.name }}
                </h2>
                <p class="text-sm text-muted-foreground">
                    {{ plugin.description || "No description provided." }}
                </p>
                <a
                    class="inline-flex items-center gap-2 text-sm font-medium text-primary break-all"
                    :href="plugin.url"
                    target="_blank"
                    rel="noreferrer noopener"
                >
                    <Link class="w-4 h-4" />
                    {{ plugin.url }}
                </a>
            </div>
            <Button variant="destructive" size="sm" :disabled="deleting" @click="openDeleteDialog">
                <Trash2 class="w-4 h-4 mr-2" />
                Delete
            </Button>
        </div>
    </div>

    <AlertDialog v-model:open="showDeleteDialog">
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Delete plugin</AlertDialogTitle>
                <AlertDialogDescription>
                    Removing "{{ plugin.name }}" will make it unavailable to all projects. This action cannot be undone.
                </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction :disabled="deleting" @click="confirmDelete">Delete</AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { Trash2, Link } from "lucide-vue-next";
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
    deleting?: boolean;
}>();

const emit = defineEmits<{
    delete: [pluginId: string];
}>();

const showDeleteDialog = ref(false);

function openDeleteDialog() {
    showDeleteDialog.value = true;
}

function confirmDelete() {
    showDeleteDialog.value = false;
    emit("delete", props.plugin.id);
}
</script>
