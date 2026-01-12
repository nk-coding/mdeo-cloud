<template>
    <Dialog :open="open" @update:open="(value) => emit('update:open', value)">
        <DialogContent>
            <DialogHeader>
                <DialogTitle>Add User</DialogTitle>
                <DialogDescription>Select a user to add as an owner to the project.</DialogDescription>
            </DialogHeader>
            <div class="py-4">
                <Input v-model="searchText" placeholder="Search users..." class="mb-2" />
                <ScrollArea class="h-75 border rounded-md">
                    <Tree class="w-full p-2" :active-element="undefined" :expanded-items="expandedItems">
                        <TreeItem
                            v-for="user in filteredUsers"
                            :key="user.id"
                            :data="user"
                            :is-folder="false"
                            :has-children="false"
                            @click="handleSelectUser(user)"
                        >
                            <template #content>
                                <UserIcon class="w-4 h-4 mr-2" />
                                <div class="flex-1 min-w-0 text-left">
                                    <div class="font-medium truncate">{{ user.username }}</div>
                                </div>
                            </template>
                        </TreeItem>
                    </Tree>
                </ScrollArea>
            </div>
        </DialogContent>
    </Dialog>
</template>

<script setup lang="ts">
import { ref, computed, inject, watch } from "vue";
import { Input } from "@/components/ui/input";
import ScrollArea from "@/components/ui/scroll-area/ScrollArea.vue";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import Tree from "@/components/tree/Tree.vue";
import TreeItem from "@/components/tree/TreeItem.vue";
import type { User, UserInfo } from "@/data/api/backendApi";
import { User as UserIcon } from "lucide-vue-next";
import { workbenchStateKey } from "@/components/workbench/util";

const props = defineProps<{
    open: boolean;
    projectId: string;
}>();

const emit = defineEmits<{
    "update:open": [value: boolean];
    usersUpdated: [];
}>();

const { backendApi } = inject(workbenchStateKey)!;

const searchText = ref("");
const expandedItems = ref<Set<any>>(new Set());
const allUsers = ref<User[]>([]);
const projectUsers = ref<UserInfo[]>([]);

const availableUsers = computed(() => {
    const projectUserIds = new Set(projectUsers.value.map((u) => u.id));
    return allUsers.value.filter((u) => !projectUserIds.has(u.id));
});

const filteredUsers = computed(() => {
    if (!searchText.value.trim()) {
        return availableUsers.value;
    }
    const search = searchText.value.toLowerCase();
    return availableUsers.value.filter((u) => u.username.toLowerCase().includes(search));
});

async function loadUsers() {
    const [allUsersResult, projectUsersResult] = await Promise.all([
        backendApi.getAllUsers(),
        backendApi.getProjectOwners(props.projectId)
    ]);

    if (allUsersResult.success) {
        allUsers.value = allUsersResult.value;
    }
    if (projectUsersResult.success) {
        projectUsers.value = projectUsersResult.value;
    }
}

async function handleSelectUser(user: User) {
    const result = await backendApi.addProjectOwner(props.projectId, user.id);
    if (result.success) {
        await loadUsers();
        emit("usersUpdated");
        emit("update:open", false);
    }
}

watch(
    () => props.open,
    async (isOpen) => {
        if (isOpen) {
            searchText.value = "";
            await loadUsers();
        }
    }
);
</script>
