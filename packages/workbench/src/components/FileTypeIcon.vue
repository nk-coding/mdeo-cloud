<template>
    <Icon
        v-if="modelValue != undefined"
        :iconNode="modelValue?.icon"
        name="FileIcon"
    />
    <FileIcon v-else />
</template>
<script setup lang="ts">
import type { WorkbenchLanguagePlugin } from "@/data/plugin/languagePlugin";
import { Icon, FileIcon } from "lucide-vue-next";

defineModel<WorkbenchLanguagePlugin | undefined>();
</script>
