<template>
    <div v-if="activeSidebar != undefined" class="h-full flex">
        <Separator orientation="vertical" />
        <SidebarPanel :label="sidebarEntries[activeSidebar].name" class="flex-1">
            <KeepAlive>
                <component :is="sidebarComponent" />
            </KeepAlive>
        </SidebarPanel>
    </div>
</template>
<script setup lang="ts">
import { computed, inject } from "vue";
import Files from "../files/Files.vue";
import Projects from "../projects/Projects.vue";
import Search from "../search/Search.vue";
import SidebarPanel from "./SidebarPanel.vue";
import { Separator } from "../ui/separator";
import { workbenchStateKey } from "../workbench/util";

const { activeSidebar } = inject(workbenchStateKey)!;

const sidebarEntries = {
    projects: {
        name: "Projects"
    },
    files: {
        name: "Files"
    },
    search: {
        name: "Search"
    }
};

const sidebarComponent = computed(() => {
    if (activeSidebar.value === "files") {
        return Files;
    } else if (activeSidebar.value === "projects") {
        return Projects;
    } else if (activeSidebar.value === "search") {
        return Search;
    }
    return null;
});
</script>
