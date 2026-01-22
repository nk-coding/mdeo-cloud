<template>
    <div
        class="flex items-center justify-between gap-2 p-2 rounded-md hover:bg-accent cursor-pointer"
        @click="toggleExpanded"
    >
        <div class="flex items-center gap-2 flex-1 min-w-0">
            <component :is="expanded ? ChevronDown : ChevronRight" class="size-4 shrink-0" />
            <span class="truncate">{{ user.username }}</span>
            <span
                v-if="user.isAdmin"
                class="ml-1 text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground font-medium"
                >Admin</span
            >
        </div>
    </div>

    <div v-if="expanded" class="pl-8 pb-2 space-y-2 text-sm text-muted-foreground">
        <div class="flex flex-col gap-1">
            <span class="font-medium text-foreground">User ID</span>
            <span class="font-mono text-xs break-all">{{ user.id }}</span>
        </div>
        <div class="flex flex-col gap-1">
            <span class="font-medium text-foreground">Admin Status</span>
            <span>{{ user.isAdmin ? "Administrator" : "Regular User" }}</span>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ChevronDown, ChevronRight } from "lucide-vue-next";
import type { User } from "@/data/api/backendApi";

const props = defineProps<{
    user: User;
}>();

const expanded = ref(false);

function toggleExpanded() {
    expanded.value = !expanded.value;
}
</script>
