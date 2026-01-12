<template>
    <div class="flex flex-col h-full">
        <SidebarPanelHeader label="Projects">
            <template #actions>
                <Button v-if="project != undefined" variant="ghost" size="icon" class="h-8 w-8" @click="handleClose">
                    <X class="w-4 h-4" />
                </Button>
            </template>
        </SidebarPanelHeader>
        <div class="px-3 pb-2">
            <Button @click="openNewProjectDialog" class="w-full mb-2">
                <Plus class="w-4 h-4 mr-2" />New Project
            </Button>
            <Input v-model="searchText" placeholder="Search projects..." />
        </div>
        <ScrollArea class="flex-1 min-h-0 w-full">
            <Tree class="flex-1 w-full p-2" :active-element="project" :expanded-items="expandedItems">
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
import { ref, computed, inject } from "vue";
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
import SidebarPanelHeader from "@/components/sidebar/SidebarPanelHeader.vue";
import type { Project } from "@/data/project/project";
import { Folder, Plus, X } from "lucide-vue-next";
import { workbenchStateKey } from "../workbench/util";

const props = defineProps<{
    projects: Project[];
}>();

const emit = defineEmits<{
    createProject: [name: string];
    close: [];
}>();

const { project } = inject(workbenchStateKey)!;

const searchText = ref("");
const expandedItems = ref<Set<any>>(new Set());
const isNewProjectDialogOpen = ref(false);
const newProjectName = ref("");

const filteredProjects = computed(() => {
    if (!searchText.value.trim()) {
        return props.projects;
    }
    const search = searchText.value.toLowerCase();
    return props.projects.filter((p) => p.name.toLowerCase().includes(search));
});

function handleSelectProject(selectedProject: Project) {
    project.value = selectedProject;
}

function openNewProjectDialog() {
    newProjectName.value = "";
    isNewProjectDialogOpen.value = true;
}

function handleCreateProject() {
    const name = newProjectName.value.trim();
    if (!name) {
        return;
    }
    emit("createProject", name);
    isNewProjectDialogOpen.value = false;
    newProjectName.value = "";
}

function handleClose() {
    emit("close");
}
</script>
