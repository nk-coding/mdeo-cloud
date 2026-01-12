<template>
    <div class="space-y-3">
        <div class="flex items-start justify-between gap-4">
            <h2 class="text-2xl font-semibold text-foreground">
                {{ plugin.name }}
            </h2>
            <slot name="actions"></slot>
        </div>
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
</template>

<script setup lang="ts">
import { Link } from "lucide-vue-next";
import type { BackendPlugin } from "@/data/api/pluginTypes";

const props = defineProps<{
    plugin: BackendPlugin;
}>();
</script>
