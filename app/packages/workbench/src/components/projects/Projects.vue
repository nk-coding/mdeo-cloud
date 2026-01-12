<template>
    <ProjectsList
        v-if="showProjectsList || project == undefined"
        :projects="projects"
        @create-project="handleCreateProject"
        @close="handleCloseToDetails"
    />
    <ProjectDetails
        v-else
        :plugins="projectPlugins"
        :users="projectUsers"
        @update-name="handleUpdateProjectName"
        @plugins-updated="handlePluginsUpdated"
        @users-updated="handleUsersUpdated"
        @delete-project="handleDeleteProject"
        @open-projects="handleOpenProjects"
    />
</template>

<script setup lang="ts">
import { ref, inject, onActivated, watch } from "vue";
import { workbenchStateKey } from "@/components/workbench/util";
import type { Project } from "@/data/project/project";
import type { BackendPlugin } from "@/data/api/pluginTypes";
import type { UserInfo } from "@/data/api/backendApi";
import ProjectsList from "./ProjectsList.vue";
import ProjectDetails from "./ProjectDetails.vue";

const { backendApi, project } = inject(workbenchStateKey)!;

const projects = ref<Project[]>([]);
const showProjectsList = ref(false);

const projectPlugins = ref<BackendPlugin[]>([]);
const projectUsers = ref<UserInfo[]>([]);

async function loadProjects() {
    const result = await backendApi.getProjects();
    if (result.success) {
        projects.value = result.value;
    }
}

async function loadProjectDetails() {
    if (!project.value) {
        return;
    }

    const [pluginsResult, usersResult] = await Promise.all([
        backendApi.getProjectPlugins(project.value.id),
        backendApi.getProjectOwners(project.value.id)
    ]);

    if (pluginsResult.success) {
        projectPlugins.value = pluginsResult.value;
    }
    if (usersResult.success) {
        projectUsers.value = usersResult.value;
    }
}

function handleOpenProjects() {
    showProjectsList.value = true;
}

function handleCloseToDetails() {
    if (project.value) {
        showProjectsList.value = false;
    }
}

async function handleCreateProject(name: string) {
    const result = await backendApi.createProject(name);
    if (result.success) {
        const created = result.value;
        project.value = created;
        await loadProjects();
    }
}

async function handleUpdateProjectName(name: string) {
    if (!project.value) {
        return;
    }

    const result = await backendApi.updateProject(project.value.id, { name });
    if (result.success) {
        const updated = result.value;
        await loadProjects();
        if (project.value?.id === updated.id) {
            project.value = updated;
        }
    }
}

function handlePluginsUpdated() {
    loadProjectDetails();
}

function handleUsersUpdated() {
    loadProjectDetails();
}

async function handleDeleteProject() {
    if (project.value == undefined) {
        return;
    }

    const result = await backendApi.deleteProject(project.value.id);
    if (result.success) {
        await loadProjects();
        project.value = undefined;
    }
}

onActivated(async () => {
    await loadProjects();
    if (project.value) {
        await loadProjectDetails();
    }
});

watch(project, async (newProject) => {
    if (newProject != undefined) {
        await loadProjectDetails();
    }
    showProjectsList.value = false;
});
</script>
