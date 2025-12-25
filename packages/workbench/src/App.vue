<template>
    <Workbench v-if="workspaceState != undefined" :workbenchState="workspaceState" />
    <div v-else>Initializing...</div>
</template>
<script setup lang="ts">
import { inject, onMounted, shallowRef } from "vue";
import { monacoApiProviderKey } from "./plugins/monacoPlugin";
import { BrowserBackendApi } from "./data/api/browserBackendApi";
import { WorkbenchState } from "./data/workbenchState";
import Workbench from "./components/workbench/Workbench.vue";
import { examplePlugin, examplePlugin2 } from "./testing/examplePlugin";
import { useColorMode } from "@vueuse/core";

const monaco = inject(monacoApiProviderKey)!;

const workspaceState = shallowRef<WorkbenchState>();

useColorMode();

onMounted(async () => {
    const monacoApi = await monaco;
    const backendApi = new BrowserBackendApi();
    workspaceState.value = new WorkbenchState(monacoApi, backendApi);
    workspaceState.value.plugins.value.set(examplePlugin.id, examplePlugin);
    workspaceState.value.plugins.value.set(examplePlugin2.id, examplePlugin2);

    const path = window.location.pathname;
    if (path !== "/") {
        const projectId = path.startsWith("/") ? path.slice(1) : path;

        const result = await backendApi.getProjects();
        if (result.success) {
            const project = result.value.find((p) => p.id === projectId);

            if (project) {
                workspaceState.value.project.value = project;
            }
        }
    }
});
</script>
