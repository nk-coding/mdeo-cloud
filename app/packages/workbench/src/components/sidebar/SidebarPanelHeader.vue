<template>
    <div class="mx-4 my-2 flex items-center justify-between min-h-8">
        <span class="text-sm font-medium text-sidebar-foreground/70">{{ label }}</span>
        <slot name="actions"></slot>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    label: string;
}>();
</script>
