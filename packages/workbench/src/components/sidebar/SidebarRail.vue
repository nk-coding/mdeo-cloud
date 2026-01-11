<template>
    <div class="flex p-1 flex-col">
        <ul class="flex flex-col gap-1 flex-1">
            <li>
                <SidebarRailButton id="projects" tooltip="Projects">
                    <Icon :showText="false" class="size-8!" />
                </SidebarRailButton>
            </li>
            <li v-if="project != undefined">
                <SidebarRailButton id="files" tooltip="Files">
                    <Files />
                </SidebarRailButton>
            </li>
            <li v-if="project != undefined">
                <SidebarRailButton id="search" tooltip="Search">
                    <Search />
                </SidebarRailButton>
            </li>
        </ul>
        <SidebarRailButton tooltip="Settings" @click="openSettings" class="mt-2">
            <Settings />
        </SidebarRailButton>
        <SidebarRailButton tooltip="Toggle Theme" @click="toggleTheme" class="mt-2">
            <component :is="theme === 'dark' ? Sun : Moon" />
        </SidebarRailButton>
    </div>

    <SettingsDialog v-model:open="isSettingsOpen" :backend-api="backendApi" />
</template>
<script setup lang="ts">
import { ref } from "vue";
import { Files, Sun, Moon, Search, Settings } from "lucide-vue-next";
import { useColorMode } from "@vueuse/core";
import Icon from "../Icon.vue";
import SidebarRailButton from "./SidebarRailButton.vue";
import { inject } from "vue";
import { workbenchStateKey } from "../workbench/util";
import SettingsDialog from "../settings/SettingsDialog.vue";

const { project, backendApi } = inject(workbenchStateKey)!;

const theme = useColorMode();
const isSettingsOpen = ref(false);

function openSettings() {
    isSettingsOpen.value = true;
}

function toggleTheme() {
    if (theme.value === "light") {
        theme.value = "dark";
    } else {
        theme.value = "light";
    }
}
</script>
