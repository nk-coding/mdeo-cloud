<template>
    <div class="flex h-full flex-col p-1">
        <SidebarRailButton tooltip="MDEO">
            <Icon :showText="false" class="size-8!" />
        </SidebarRailButton>
        <div class="flex-1" />
        <SidebarRailButton tooltip="Toggle Theme" @click="toggleTheme" class="mt-2">
            <component :is="theme === 'dark' ? Sun : Moon" />
        </SidebarRailButton>
    </div>
</template>
<script setup lang="ts">
import { Sun, Moon } from "lucide-vue-next";
import { useColorMode } from "@vueuse/core";
import SidebarRailButton from "./SidebarRailButton.vue";
import Icon from "../Icon.vue";

const theme = useColorMode();

function toggleTheme() {
    theme.value = theme.value === "light" ? "dark" : "light";
}
</script>
