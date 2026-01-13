<template>
    <Icon v-if="modelValue != undefined" :iconNode="modelValue?.icon" name="FileIcon" />
    <FileIcon v-else />
</template>
<script setup lang="ts">
import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";
import { Icon, FileIcon } from "lucide-vue-next";

defineModel<ResolvedWorkbenchLanguagePlugin | undefined>();
</script>
