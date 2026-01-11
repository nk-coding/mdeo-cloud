<template>
    <Workbench
        v-if="workspaceState != undefined"
        :workbenchState="workspaceState"
        :auth-state="authState"
    />
    <div v-else-if="!isCheckingAuth" class="flex h-screen w-screen">
        <div class="flex h-full border-r border-border/60">
            <BaseSidebarRail />
        </div>
        <div class="flex flex-1 items-center justify-center px-4 py-10">
            <LoginCard
                :auth-state="authState"
                @success="handleAuthSuccess"
            />
        </div>
    </div>
</template>
<script setup lang="ts">
import { inject, shallowRef, onMounted, ref } from "vue";
import { monacoApiProviderKey } from "./lib/monacoPlugin";
import { HttpBackendApi } from "./data/api/httpBackendApi";
import { WorkbenchState } from "./data/workbenchState";
import { AuthState } from "./data/authState";
import Workbench from "./components/workbench/Workbench.vue";
import LoginCard from "./components/auth/LoginCard.vue";
import BaseSidebarRail from "./components/sidebar/BaseSidebarRail.vue";
import { useColorMode } from "@vueuse/core";

const monaco = inject(monacoApiProviderKey)!;
const backendApi = new HttpBackendApi();
const workspaceState = shallowRef<WorkbenchState>();

const authState = new AuthState(backendApi, () => {
    workspaceState.value = undefined;
});

const isCheckingAuth = ref(true)

useColorMode();

onMounted(async () => {
    await authState.checkAuthentication();
    if (authState.isAuthenticated.value) {
        await initializeWorkbench();
    }
    isCheckingAuth.value = false;
});

async function handleAuthSuccess() {
    await initializeWorkbench();
}

async function initializeWorkbench() {
    const monacoApi = await monaco;
    const state = new WorkbenchState(monacoApi, backendApi);

    await loadPlugins(state);
    workspaceState.value = state;
    await syncProjectFromPath(state);
}

async function loadPlugins(state: WorkbenchState) {
    const [
        { metamodelPlugin },
        { scriptPlugin },
        { modelPlugin },
        { modelTransformationPlugin },
        { configPlugin }
    ] = await Promise.all([
        import("./plugins/metamodelPlugin"),
        import("./plugins/scriptPlugin"),
        import("./plugins/modelPlugin"),
        import("./plugins/modelTransformationPlugin"),
        import("./plugins/configPlugin")
    ]);

    state.plugins.value.set(metamodelPlugin.id, metamodelPlugin);
    state.plugins.value.set(scriptPlugin.id, scriptPlugin);
    state.plugins.value.set(modelPlugin.id, modelPlugin);
    state.plugins.value.set(modelTransformationPlugin.id, modelTransformationPlugin);
    state.plugins.value.set(configPlugin.id, configPlugin);
}

async function syncProjectFromPath(state: WorkbenchState) {
    const path = window.location.pathname;
    if (path === "/") {
        return;
    }
    const projectId = path.startsWith("/") ? path.slice(1) : path;
    const result = await backendApi.getProjects();
    if (!result.success) {
        return;
    }
    const project = result.value.find((p) => p.id === projectId);
    if (project) {
        state.project.value = project;
    }
}
</script>
