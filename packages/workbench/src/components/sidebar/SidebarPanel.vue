<template>
    <div class="h-full flex flex-col">
        <span class="mx-3 mt-2 text-sm font-medium text-sidebar-foreground/70">{{ label }}</span>
        <slot></slot>
    </div>
</template>
<script setup lang="ts">
defineProps<{
    label: string;
}>();
</script>
