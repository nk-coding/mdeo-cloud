<template>
    <div class="h-full w-full flex flex-col">
        <span class="mx-4 mt-3 text-sm font-medium text-sidebar-foreground/70">{{ label }}</span>
        <slot></slot>
    </div>
</template>
<script setup lang="ts">
defineProps<{
    label: string;
}>();
</script>
