<template>
    <div class="flex flex-col h-full">
        <SidebarPanelHeader label="Project">
            <template #actions>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button variant="ghost" size="icon" class="h-8 w-8" @click="handleOpenProjects">
                            <FolderOpen class="size-4" />
                        </Button>
                    </TooltipTrigger>
                    <TooltipContent side="right">Open Projects</TooltipContent>
                </Tooltip>
            </template>
        </SidebarPanelHeader>

        <div class="flex-1 flex flex-col min-h-0">
            <div class="px-4 pb-3 space-y-3 relative">
                <div class="flex items-center gap-2">
                    <div v-if="isEditingName" class="flex-1 -m-1">
                        <Input
                            v-model="editedName"
                            ref="projectNameInput"
                            @blur="handleSaveName"
                            @keydown.enter="handleSaveName"
                        />
                    </div>
                    <span v-else class="text-base font-semibold flex-1 ml-px">{{ project!.name }}</span>
                    <div v-if="!isEditingName" class="absolute right-4 top-0">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button variant="ghost" size="icon" class="h-8 w-8" @click="handleEditName">
                                    <Pencil class="size-4" />
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent side="right">Edit Name</TooltipContent>
                        </Tooltip>
                    </div>
                </div>
            </div>

            <div class="flex-1 flex flex-col min-h-0 overflow-y-auto">
                <div class="flex flex-col min-h-0 pb-2">
                    <Separator />
                    <SidebarPanelHeader label="Plugins">
                        <template #actions>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        class="h-8 w-8"
                                        @click="openManagePluginsDialog"
                                    >
                                        <Settings2 class="size-4" />
                                    </Button>
                                </TooltipTrigger>
                                <TooltipContent side="right">Manage Plugins</TooltipContent>
                            </Tooltip>
                        </template>
                    </SidebarPanelHeader>
                    <ScrollArea class="flex-1 min-h-0 px-2">
                        <Tree
                            v-if="sortedPlugins.length > 0"
                            class="w-full"
                            :active-element="null"
                            :expanded-items="new Set()"
                        >
                            <TreeItem
                                v-for="plugin in sortedPlugins"
                                :key="plugin.id"
                                :data="plugin"
                                :is-folder="false"
                                :has-children="false"
                                @click="handlePluginClick(plugin as WorkbenchPlugin)"
                            >
                                <template #content>
                                    <Icon :iconNode="plugin.icon" name="PluginIcon" class="size-4 mr-2" />
                                    <span class="truncate">{{ plugin.name }}</span>
                                </template>
                            </TreeItem>
                        </Tree>
                        <div v-else class="text-sm text-muted-foreground p-2">No plugins added</div>
                    </ScrollArea>
                </div>

                <div class="flex flex-col min-h-0 pb-2">
                    <Separator />
                    <SidebarPanelHeader label="Users">
                        <template #actions>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button variant="ghost" size="icon" class="h-8 w-8" @click="openAddUserDialog">
                                        <Plus class="size-4" />
                                    </Button>
                                </TooltipTrigger>
                                <TooltipContent side="right">Add User</TooltipContent>
                            </Tooltip>
                        </template>
                    </SidebarPanelHeader>
                    <ScrollArea class="flex-1 min-h-0 px-2">
                        <Tree v-if="users.length > 0" class="w-full" :active-element="null" :expanded-items="new Set()">
                            <ContextMenu v-for="user in users" :key="user.id">
                                <ContextMenuTrigger>
                                    <TreeItem :data="user" :is-folder="false" :has-children="false">
                                        <template #content>
                                            <UserIcon class="size-4 mr-2" />
                                            <span class="truncate">{{ user.username }}</span>
                                        </template>
                                    </TreeItem>
                                </ContextMenuTrigger>
                                <ContextMenuContent>
                                    <ContextMenuItem @click="handleRemoveUser(user)" :disabled="users.length <= 1">
                                        <Trash2 class="size-4 mr-2" />
                                        Remove
                                    </ContextMenuItem>
                                </ContextMenuContent>
                            </ContextMenu>
                        </Tree>
                        <div v-else class="text-sm text-muted-foreground p-2">No users</div>
                    </ScrollArea>
                </div>

                <div>
                    <Separator />
                    <SidebarPanelHeader label="Management" />
                    <div class="px-4 py-2">
                        <Button variant="destructive" class="w-full" @click="openDeleteDialog">
                            <Trash2 class="size-4 mr-2" />
                            Delete Project
                        </Button>
                    </div>
                </div>
            </div>
        </div>

        <ManagePluginsDialog
            v-model:open="isManagePluginsDialogOpen"
            :project-id="project!.id"
            :selected-plugin-id-prop="selectedPluginIdForDialog"
        />

        <AddUserDialog
            v-model:open="isAddUserDialogOpen"
            :project-id="project!.id"
            @users-updated="handleUsersUpdated"
        />

        <AlertDialog v-model:open="isDeleteDialogOpen">
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Delete Project</AlertDialogTitle>
                    <AlertDialogDescription>
                        Are you sure you want to delete "{{ project!.name }}"? This action cannot be undone and will
                        permanently delete all files in this project.
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction @click="handleDeleteProject">Delete</AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    </div>
</template>

<script setup lang="ts">
import { ref, inject, useTemplateRef, nextTick, computed } from "vue";
import { Input } from "@/components/ui/input";
import { Tooltip, TooltipTrigger, TooltipContent } from "@/components/ui/tooltip";
import { Button } from "@/components/ui/button";
import ScrollArea from "@/components/ui/scroll-area/ScrollArea.vue";
import { Separator } from "@/components/ui/separator";
import SidebarPanelHeader from "@/components/sidebar/SidebarPanelHeader.vue";
import Tree from "@/components/tree/Tree.vue";
import TreeItem from "@/components/tree/TreeItem.vue";
import { ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger } from "@/components/ui/context-menu";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle
} from "@/components/ui/alert-dialog";
import ManagePluginsDialog from "./ManagePluginsDialog.vue";
import AddUserDialog from "./AddUserDialog.vue";
import type { UserInfo } from "@/data/api/backendApi";
import { Pencil, Plus, Trash2, FolderOpen, User as UserIcon, Settings2, Icon } from "lucide-vue-next";
import { workbenchStateKey } from "@/components/workbench/util";
import type { WorkbenchPlugin } from "@/data/plugin/plugin";
import { showApiError } from "@/lib/notifications";

const props = defineProps<{
    users: UserInfo[];
}>();

const emit = defineEmits<{
    updateName: [name: string];
    usersUpdated: [];
    deleteProject: [];
    openProjects: [];
}>();

const { backendApi, project, plugins } = inject(workbenchStateKey)!;

const isEditingName = ref(false);
const editedName = ref("");
const isManagePluginsDialogOpen = ref(false);
const isAddUserDialogOpen = ref(false);
const isDeleteDialogOpen = ref(false);
const projectNameInput = useTemplateRef("projectNameInput");
const selectedPluginIdForDialog = ref<string | undefined>(undefined);

const sortedPlugins = computed(() => {
    return Array.from(plugins.value.values()).sort((a, b) => a.name.localeCompare(b.name));
});

function handleEditName() {
    editedName.value = project.value!.name;
    isEditingName.value = true;
    nextTick(() => {
        projectNameInput.value?.$el.focus();
    });
}

function handleSaveName() {
    if (editedName.value.trim() && editedName.value !== project.value!.name) {
        emit("updateName", editedName.value.trim());
    }
    isEditingName.value = false;
}

function openManagePluginsDialog() {
    selectedPluginIdForDialog.value = undefined;
    isManagePluginsDialogOpen.value = true;
}

function openAddUserDialog() {
    isAddUserDialogOpen.value = true;
}

function openDeleteDialog() {
    isDeleteDialogOpen.value = true;
}

function handleUsersUpdated() {
    emit("usersUpdated");
}

async function handleRemoveUser(user: UserInfo) {
    if (props.users.length <= 1) return;

    const result = await backendApi.projects.removeOwner(project.value!.id, user.id);
    if (result.success) {
        emit("usersUpdated");
    } else {
        showApiError("remove user from project", result.error.message);
    }
}

function handleDeleteProject() {
    emit("deleteProject");
}

function handleOpenProjects() {
    emit("openProjects");
}

function handlePluginClick(plugin: WorkbenchPlugin) {
    selectedPluginIdForDialog.value = plugin.id;
    isManagePluginsDialogOpen.value = true;
}
</script>
