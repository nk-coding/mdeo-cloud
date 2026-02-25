<template>
    <Dialog :open="open" @update:open="(value) => emit('update:open', value)">
        <DialogContent class="sm:max-w-4xl w-full h-125 flex flex-col p-0">
            <DialogHeader class="p-6 pb-0">
                <DialogTitle>Manage Users</DialogTitle>
                <DialogDescription>Manage project members and their project permissions.</DialogDescription>
            </DialogHeader>
            <div class="flex gap-6 p-6 pt-4 min-h-0 flex-1">
                <Card class="p-4 gap-0 w-80">
                    <SidebarMenu class="min-w-0">
                        <SidebarMenuItem>
                            <SidebarMenuButtonChild
                                class="gap-2 justify-start"
                                :is-active="selectedEntry === 'add'"
                                @click="selectedEntry = 'add'"
                            >
                                <Plus class="size-4" />
                                <span class="text-sm font-medium">Add user</span>
                            </SidebarMenuButtonChild>
                        </SidebarMenuItem>
                    </SidebarMenu>

                    <SidebarInput v-model="searchText" placeholder="Search users..." class="mt-4 bg-transparent" />

                    <div class="mt-4 flex-1 min-h-0 overflow-hidden">
                        <ScrollArea class="h-full **:data-[slot=scroll-area-viewport]:rounded-none">
                            <SidebarMenu class="pb-2">
                                <SidebarMenuItem v-for="user in filteredProjectUsers" :key="user.id">
                                    <SidebarMenuButtonChild
                                        class="justify-start gap-3"
                                        :is-active="selectedEntry === user.id"
                                        @click="selectedEntry = user.id"
                                    >
                                        <UserIcon class="size-4 shrink-0" />
                                        <span class="text-sm font-medium truncate">{{ user.username }}</span>
                                        <div class="ml-auto flex items-center gap-1 shrink-0">
                                            <span
                                                v-if="user.isAdmin"
                                                class="text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground font-medium"
                                                >Admin</span
                                            >
                                            <span
                                                v-if="!user.isAdmin && user.canExecute"
                                                class="text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground font-medium"
                                                >Execute</span
                                            >
                                            <span
                                                v-if="!user.isAdmin && user.canWrite"
                                                class="text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground font-medium"
                                                >Write</span
                                            >
                                        </div>
                                    </SidebarMenuButtonChild>
                                </SidebarMenuItem>
                            </SidebarMenu>

                            <div v-if="isLoading" class="py-6 text-center text-sm text-muted-foreground">
                                Loading users...
                            </div>
                            <div
                                v-else-if="filteredProjectUsers.length === 0"
                                class="py-6 text-center text-sm text-muted-foreground"
                            >
                                {{ searchText.trim() ? "No users match your search." : "No users in this project." }}
                            </div>
                        </ScrollArea>
                    </div>
                </Card>

                <Card class="p-6 gap-4 flex-1">
                    <template v-if="selectedEntry === 'add'">
                        <div>
                            <h3 class="text-lg font-medium">Add User</h3>
                            <p class="text-sm text-muted-foreground">Select a user to add to this project.</p>
                        </div>
                        <Input v-model="addSearchText" placeholder="Search users to add..." />
                        <ScrollArea class="flex-1 min-h-0">
                            <div v-if="availableUsers.length === 0" class="p-4 text-sm text-muted-foreground">
                                No available users to add.
                            </div>
                            <div v-else class="space-y-1">
                                <div
                                    v-for="user in filteredAvailableUsers"
                                    :key="user.id"
                                    class="flex items-center justify-between gap-3 py-2"
                                >
                                    <div class="min-w-0 flex items-center gap-2">
                                        <UserIcon class="size-4 text-muted-foreground shrink-0" />
                                        <div class="text-sm font-medium truncate">{{ user.username }}</div>
                                    </div>
                                    <Button size="sm" @click="handleAddUser(user.id)" :disabled="isProcessing"
                                        >Add</Button
                                    >
                                </div>
                            </div>
                        </ScrollArea>
                    </template>

                    <template v-else-if="selectedProjectUser">
                        <div class="flex items-center justify-between gap-3">
                            <div>
                                <h3 class="text-lg font-medium">{{ selectedProjectUser.username }}</h3>
                                <p class="text-sm text-muted-foreground">Update project permissions for this user.</p>
                            </div>
                            <Button
                                variant="destructive"
                                size="sm"
                                :disabled="isProcessing || isLastProjectAdmin"
                                @click="handleRemoveSelectedUser"
                            >
                                <Trash2 class="size-4 mr-2" />
                                Remove
                            </Button>
                        </div>

                        <Separator />

                        <div class="space-y-4">
                            <div class="flex items-center justify-between">
                                <div>
                                    <div class="text-sm font-medium">Admin</div>
                                    <p class="text-xs text-muted-foreground">
                                        Project admins have full project access.
                                    </p>
                                </div>
                                <Switch
                                    :model-value="selectedProjectUser.isAdmin"
                                    :disabled="isProcessing || isLastProjectAdmin"
                                    @update:model-value="handleAdminChange"
                                />
                            </div>
                            <div class="flex items-center justify-between">
                                <div>
                                    <div class="text-sm font-medium">Execute</div>
                                    <p class="text-xs text-muted-foreground">Allows running project executions.</p>
                                </div>
                                <Switch
                                    :model-value="selectedProjectUser.canExecute"
                                    :disabled="isProcessing || selectedProjectUser.isAdmin"
                                    @update:model-value="handleCanExecuteChange"
                                />
                            </div>
                            <div class="flex items-center justify-between">
                                <div>
                                    <div class="text-sm font-medium">Write</div>
                                    <p class="text-xs text-muted-foreground">
                                        Allows creating and editing project files.
                                    </p>
                                </div>
                                <Switch
                                    :model-value="selectedProjectUser.canWrite"
                                    :disabled="isProcessing || selectedProjectUser.isAdmin"
                                    @update:model-value="handleCanWriteChange"
                                />
                            </div>
                        </div>
                    </template>
                </Card>
            </div>
        </DialogContent>
    </Dialog>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from "vue";
import { Plus, Trash2, User as UserIcon } from "lucide-vue-next";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { SidebarInput, SidebarMenu, SidebarMenuItem } from "@/components/ui/sidebar";
import SidebarMenuButtonChild from "@/components/ui/sidebar/SidebarMenuButtonChild.vue";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { Switch } from "@/components/ui/switch";
import { workbenchStateKey } from "@/components/workbench/util";
import type { ProjectUserInfo, User } from "@/data/api/backendApi";
import { showApiError } from "@/lib/notifications";

const props = defineProps<{
    open: boolean;
    projectId: string;
    selectedUserIdProp?: string;
}>();

const emit = defineEmits<{
    "update:open": [value: boolean];
    usersUpdated: [];
}>();

const { backendApi } = inject(workbenchStateKey)!;

const isLoading = ref(false);
const isProcessing = ref(false);
const searchText = ref("");
const addSearchText = ref("");
const selectedEntry = ref<"add" | string>("add");
const projectUsers = ref<ProjectUserInfo[]>([]);
const allUsers = ref<User[]>([]);

const filteredProjectUsers = computed(() => {
    const search = searchText.value.trim().toLowerCase();
    const users = [...projectUsers.value].sort((a, b) => a.username.localeCompare(b.username));
    if (!search) {
        return users;
    }
    return users.filter(
        (user) => user.username.toLowerCase().includes(search) || user.id.toLowerCase().includes(search)
    );
});

const selectedProjectUser = computed(() => {
    if (selectedEntry.value === "add") {
        return undefined;
    }
    return projectUsers.value.find((user) => user.id === selectedEntry.value);
});

const availableUsers = computed(() => {
    const projectUserIds = new Set(projectUsers.value.map((user) => user.id));
    return allUsers.value
        .filter((user) => !projectUserIds.has(user.id))
        .sort((a, b) => a.username.localeCompare(b.username));
});

const filteredAvailableUsers = computed(() => {
    const search = addSearchText.value.trim().toLowerCase();
    if (!search) {
        return availableUsers.value;
    }
    return availableUsers.value.filter(
        (user) => user.username.toLowerCase().includes(search) || user.id.toLowerCase().includes(search)
    );
});

const isLastProjectAdmin = computed(() => {
    if (!selectedProjectUser.value?.isAdmin) {
        return false;
    }
    const adminUsers = projectUsers.value.filter((user) => user.isAdmin);
    return adminUsers.length === 1 && adminUsers[0]?.id === selectedProjectUser.value.id;
});

async function reloadUsers() {
    isLoading.value = true;
    try {
        const [projectUsersResult, allUsersResult] = await Promise.all([
            backendApi.projects.getUsers(props.projectId),
            backendApi.users.getAll()
        ]);

        if (projectUsersResult.success) {
            projectUsers.value = projectUsersResult.value;
        } else {
            showApiError("load project users", projectUsersResult.error.message);
        }

        if (allUsersResult.success) {
            allUsers.value = allUsersResult.value;
        } else {
            showApiError("load users", allUsersResult.error.message);
        }

        if (
            selectedEntry.value !== "add" &&
            !projectUsers.value.some((projectUser) => projectUser.id === selectedEntry.value)
        ) {
            selectedEntry.value = "add";
        }
    } finally {
        isLoading.value = false;
    }
}

async function handleAddUser(userId: string) {
    isProcessing.value = true;
    try {
        const result = await backendApi.projects.addUser(props.projectId, userId, false, false, false);
        if (result.success) {
            await reloadUsers();
            selectedEntry.value = userId;
            emit("usersUpdated");
        } else {
            showApiError("add user to project", result.error.message);
        }
    } finally {
        isProcessing.value = false;
    }
}

async function saveSelectedUserPermissions(next: ProjectUserInfo) {
    isProcessing.value = true;
    try {
        const result = await backendApi.projects.updateUserPermissions(
            props.projectId,
            next.id,
            next.isAdmin,
            next.canExecute,
            next.canWrite
        );
        if (result.success) {
            const existing = projectUsers.value.find((user) => user.id === next.id);
            if (existing) {
                existing.isAdmin = next.isAdmin;
                existing.canExecute = next.canExecute;
                existing.canWrite = next.canWrite;
            }
            emit("usersUpdated");
        } else {
            showApiError("update project user permissions", result.error.message);
            await reloadUsers();
        }
    } finally {
        isProcessing.value = false;
    }
}

async function handleAdminChange(value: boolean) {
    const user = selectedProjectUser.value;
    if (!user) {
        return;
    }
    const next: ProjectUserInfo = {
        ...user,
        isAdmin: value,
        canExecute: value ? true : user.canExecute,
        canWrite: value ? true : user.canWrite
    };
    await saveSelectedUserPermissions(next);
}

async function handlePermissionChange(type: "canExecute" | "canWrite", value: boolean) {
    const user = selectedProjectUser.value;
    if (!user) {
        return;
    }
    const next: ProjectUserInfo = {
        ...user,
        [type]: value
    };
    await saveSelectedUserPermissions(next);
}

async function handleCanExecuteChange(value: boolean) {
    await handlePermissionChange("canExecute", value);
}

async function handleCanWriteChange(value: boolean) {
    await handlePermissionChange("canWrite", value);
}

async function handleRemoveSelectedUser() {
    const user = selectedProjectUser.value;
    if (!user) {
        return;
    }

    isProcessing.value = true;
    try {
        const result = await backendApi.projects.removeUser(props.projectId, user.id);
        if (result.success) {
            selectedEntry.value = "add";
            await reloadUsers();
            emit("usersUpdated");
        } else {
            showApiError("remove user from project", result.error.message);
        }
    } finally {
        isProcessing.value = false;
    }
}

watch(
    () => props.open,
    async (isOpen) => {
        if (isOpen) {
            searchText.value = "";
            addSearchText.value = "";
            selectedEntry.value = props.selectedUserIdProp ?? "add";
            await reloadUsers();
        }
    }
);
</script>
