<template>
    <div v-if="activeSidebar != undefined" class="h-full flex">
        <Separator orientation="vertical" />
        <SidebarPanel class="flex-1">
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
import Executions from "../executions/Executions.vue";
import SidebarPanel from "./SidebarPanel.vue";
import { Separator } from "../ui/separator";
import { workbenchStateKey } from "../workbench/util";

const { activeSidebar } = inject(workbenchStateKey)!;

const sidebarComponent = computed(() => {
    if (activeSidebar.value === "files") {
        return Files;
    } else if (activeSidebar.value === "projects") {
        return Projects;
    } else if (activeSidebar.value === "search") {
        return Search;
    } else if (activeSidebar.value === "executions") {
        return Executions;
    }
    return null;
});
</script>
