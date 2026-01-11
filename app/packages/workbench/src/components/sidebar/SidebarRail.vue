<template>
    <div class="flex p-1 flex-col">
        <ul class="flex flex-col gap-1 flex-1">
            <li>
                <SidebarRailButton
                    tooltip="Projects"
                    :active="activeSidebar === 'projects'"
                    @click="toggleSidebar('projects')"
                >
                    <Icon :showText="false" class="size-8!" />
                </SidebarRailButton>
            </li>
            <li v-if="project != undefined">
                <SidebarRailButton tooltip="Files" :active="activeSidebar === 'files'" @click="toggleSidebar('files')">
                    <Files />
                </SidebarRailButton>
            </li>
            <li v-if="project != undefined">
                <SidebarRailButton
                    tooltip="Search"
                    :active="activeSidebar === 'search'"
                    @click="toggleSidebar('search')"
                >
                    <Search />
                </SidebarRailButton>
            </li>
        </ul>
        <template v-if="isAdmin">
            <SidebarRailButton tooltip="Settings" @click="openSettings" class="mt-2">
                <Settings />
            </SidebarRailButton>
        </template>
        <SidebarRailButton tooltip="Account" @click="openAccount" class="mt-2">
            <UserRound />
        </SidebarRailButton>
        <SidebarRailButton tooltip="Toggle Theme" @click="toggleTheme" class="mt-2">
            <component :is="theme === 'dark' ? Sun : Moon" />
        </SidebarRailButton>
    </div>

    <SettingsDialog v-if="isAdmin" v-model:open="isSettingsOpen" :backend-api="backendApi" />
    <AccountDialog v-model:open="isAccountOpen" />
</template>
<script setup lang="ts">
import { inject, ref } from "vue";
import { Files, Sun, Moon, Search, Settings, UserRound } from "lucide-vue-next";
import { useColorMode } from "@vueuse/core";
import Icon from "../Icon.vue";
import SidebarRailButton from "./SidebarRailButton.vue";
import { workbenchStateKey, authStateKey } from "../workbench/util";
import SettingsDialog from "../settings/SettingsDialog.vue";
import AccountDialog from "../account/AccountDialog.vue";
import type { SidebarType } from "@/data/workbenchState";

const workbench = inject(workbenchStateKey)!;
const { project, backendApi, activeSidebar, sidebarCollapsed } = workbench;
const authState = inject(authStateKey)!;
const isAdmin = authState.user.value?.isAdmin ?? false;

const theme = useColorMode();
const isSettingsOpen = ref(false);
const isAccountOpen = ref(false);

function openSettings() {
    isSettingsOpen.value = true;
}

function openAccount() {
    isAccountOpen.value = true;
}

function toggleSidebar(target: SidebarType) {
    if (activeSidebar.value === target) {
        sidebarCollapsed.value = !sidebarCollapsed.value;
        return;
    }
    activeSidebar.value = target;
    sidebarCollapsed.value = false;
}

function toggleTheme() {
    if (theme.value === "light") {
        theme.value = "dark";
    } else {
        theme.value = "light";
    }
}
</script>
