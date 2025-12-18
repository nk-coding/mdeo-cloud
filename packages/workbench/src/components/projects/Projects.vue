<template>
    <div class="flex flex-col h-full">
        <div class="px-3 pt-3 pb-2">
            <Button @click="openNewProjectDialog" class="w-full mb-2">
                <Plus class="w-4 h-4 mr-2" />New Project
            </Button>
            <Input v-model="searchText" placeholder="Search projects..." />
        </div>
        <ScrollArea class="flex-1 min-h-0 w-full">
            <Tree class="flex-1 w-full p-2" :active-element="activeProject" :expanded-items="expandedItems">
                <TreeItem
                    v-for="project in filteredProjects"
                    :key="project.id"
                    :data="project"
                    :is-folder="false"
                    :has-children="false"
                    @click="handleSelectProject(project)"
                >
                    <template #content>
                        <Folder class="w-4 h-4 mr-2" />
                        <span>{{ project.name }}</span>
                    </template>
                </TreeItem>
            </Tree>
        </ScrollArea>

        <Dialog v-model:open="isNewProjectDialogOpen">
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Create New Project</DialogTitle>
                    <DialogDescription> Enter a name for your new project. </DialogDescription>
                </DialogHeader>
                <div class="py-4">
                    <Input v-model="newProjectName" placeholder="Project name" @keydown.enter="handleCreateProject" />
                </div>
                <DialogFooter>
                    <Button variant="outline" @click="isNewProjectDialogOpen = false"> Cancel </Button>
                    <Button @click="handleCreateProject" :disabled="!newProjectName.trim()"> Create </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, onActivated } from "vue";
import Tree from "@/components/tree/Tree.vue";
import TreeItem from "@/components/tree/TreeItem.vue";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import ScrollArea from "@/components/ui/scroll-area/ScrollArea.vue";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle
} from "@/components/ui/dialog";
import { workbenchStateKey } from "@/components/workbench/util";
import type { Project } from "@/data/project/project";
import { Folder, Plus } from "lucide-vue-next";

const { backendApi, project } = inject(workbenchStateKey)!;

const projects = ref<Project[]>([]);
const searchText = ref("");
const activeProject = computed(() => project.value);
const expandedItems = ref<Set<any>>(new Set());

const isNewProjectDialogOpen = ref(false);
const newProjectName = ref("");

const filteredProjects = computed(() => {
    if (!searchText.value.trim()) {
        return projects.value;
    }
    const search = searchText.value.toLowerCase();
    return projects.value.filter((p) => p.name.toLowerCase().includes(search));
});

async function loadProjects() {
    const result = await backendApi.getProjects();
    if (result.success) {
        projects.value = result.value;
    }
}

function handleSelectProject(selectedProject: Project) {
    project.value = selectedProject;
}

function openNewProjectDialog() {
    newProjectName.value = "";
    isNewProjectDialogOpen.value = true;
}

async function handleCreateProject() {
    const name = newProjectName.value.trim();
    if (!name) {
        return;
    }

    const result = await backendApi.createProject(name);
    if (result.success) {
        await loadProjects();
        isNewProjectDialogOpen.value = false;
        newProjectName.value = "";
    }
}

onActivated(async () => {
    await loadProjects();
});
</script>
