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
import { examplePlugin } from "./testing/examplePlugin";

const monaco = inject(monacoApiProviderKey)!;

const workspaceState = shallowRef<WorkbenchState>();

onMounted(async () => {
    const monacoApi = await monaco;
    const backendApi = new BrowserBackendApi();
    workspaceState.value = new WorkbenchState(monacoApi, backendApi);
    workspaceState.value.plugins.value.set(examplePlugin.id, examplePlugin);
});
</script>
