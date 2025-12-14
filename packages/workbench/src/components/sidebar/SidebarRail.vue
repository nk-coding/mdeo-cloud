<template>
    <div class="flex p-1 flex-col">
        <ul class="flex flex-col gap-1 flex-1">
            <li>
                <SidebarRailButton id="projects" tooltip="Projects">
                    <Icon :showText="false" class="size-8!" />
                </SidebarRailButton>
            </li>
            <li>
                <SidebarRailButton id="files" tooltip="Files">
                    <File />
                </SidebarRailButton>
            </li>
            <li>
                <SidebarRailButton id="search" tooltip="Search">
                    <Search />
                </SidebarRailButton>
            </li>
        </ul>
        <SidebarRailButton tooltip="Toggle Theme" @click="toggleTheme" class="mt-2">
            <component :is="theme === 'dark' ? Sun : Moon" />
        </SidebarRailButton>
    </div>
</template>
<script setup lang="ts">
import { File, Sun, Moon, Search } from "lucide-vue-next";
import { useColorMode } from "@vueuse/core";
import Icon from "../Icon.vue";
import SidebarRailButton from "./SidebarRailButton.vue";

const theme = useColorMode();

function toggleTheme() {
    if (theme.value === "light") {
        theme.value = "dark";
    } else {
        theme.value = "light";
    }
}
</script>
