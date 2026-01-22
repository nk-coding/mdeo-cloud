<template>
    <div class="h-full flex flex-col">
        <div class="flex flex-1 min-h-0 gap-6">
            <Card class="p-4 gap-0">
                <SidebarInput v-model="searchText" placeholder="Search users..." class="bg-transparent" />

                <div class="mt-4 flex-1 min-h-0 overflow-hidden">
                    <ScrollArea class="h-full pr-1 **:data-[slot=scroll-area-viewport]:rounded-none">
                        <SidebarMenu class="pb-2">
                            <SidebarMenuItem v-for="user in filteredUsers" :key="user.id">
                                <SidebarMenuButtonChild
                                    class="justify-start gap-3"
                                    :is-active="selectedUserId === user.id"
                                    @click="selectUser(user.id)"
                                >
                                    <User class="size-4 shrink-0" />
                                    <span class="text-sm font-medium truncate">{{ user.username }}</span>
                                    <span
                                        v-if="user.isAdmin"
                                        class="ml-auto text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground font-medium"
                                        >Admin</span
                                    >
                                </SidebarMenuButtonChild>
                            </SidebarMenuItem>
                        </SidebarMenu>

                        <div v-if="isLoading" class="py-6 text-center text-sm text-muted-foreground">
                            Loading users...
                        </div>
                        <div
                            v-else-if="filteredUsers.length === 0"
                            class="py-6 text-center text-sm text-muted-foreground"
                        >
                            {{ searchText.trim() ? "No users match your search." : "No users found." }}
                        </div>
                    </ScrollArea>
                </div>
            </Card>

            <Card class="p-0 gap-0 flex-1">
                <ScrollArea class="h-full **:data-[slot=scroll-area-viewport]:rounded-none">
                    <div class="p-6">
                        <section v-if="selectedUser" class="space-y-6">
                            <UserDetails
                                :user="selectedUser"
                                :backend-api="backendApi"
                                @admin-updated="handleAdminUpdated"
                            />
                        </section>

                        <section v-else class="py-10 text-center text-sm text-muted-foreground">
                            Select a user from the sidebar to view their details.
                        </section>
                    </div>
                </ScrollArea>
            </Card>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { User } from "lucide-vue-next";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Card } from "@/components/ui/card";
import { SidebarMenu, SidebarMenuItem, SidebarInput } from "@/components/ui/sidebar";
import SidebarMenuButtonChild from "@/components/ui/sidebar/SidebarMenuButtonChild.vue";
import UserDetails from "./UserDetails.vue";
import type { BackendApi } from "@/data/api/backendApi";
import type { User as UserType } from "@/data/api/backendApi";

const props = defineProps<{
    backendApi: BackendApi;
}>();

const users = ref<UserType[]>([]);
const searchText = ref("");
const isLoading = ref(false);
const selectedUserId = ref<string>();
const hasLoaded = ref(false);

const filteredUsers = computed(() => {
    if (!searchText.value.trim()) {
        return users.value;
    }
    const search = searchText.value.toLowerCase();
    return users.value.filter((u) => u.username.toLowerCase().includes(search) || u.id.toLowerCase().includes(search));
});

const selectedUser = computed(() => {
    if (!selectedUserId.value) {
        return null;
    }
    return users.value.find((user) => user.id === selectedUserId.value) ?? null;
});

watch(filteredUsers, (list) => {
    if (!hasLoaded.value) {
        return;
    }
    if (list.length === 0) {
        selectedUserId.value = undefined;
        return;
    }
    if (!selectedUserId.value) {
        return;
    }
    if (!list.some((user) => user.id === selectedUserId.value)) {
        const firstVisible = list[0];
        if (firstVisible) {
            selectedUserId.value = firstVisible.id;
        }
    }
});

function selectUser(userId: string) {
    selectedUserId.value = userId;
}

async function loadUsers(preferredSelection?: string) {
    isLoading.value = true;
    try {
        const result = await props.backendApi.users.getAll();
        if (result.success) {
            users.value = result.value.sort((a, b) => {
                return a.username.localeCompare(b.username);
            });
            hasLoaded.value = true;
            if (preferredSelection) {
                selectedUserId.value = preferredSelection;
            } else if (users.value.length === 0) {
                selectedUserId.value = undefined;
            } else if (selectedUserId.value && !users.value.some((user) => user.id === selectedUserId.value)) {
                const firstUser = users.value[0];
                if (firstUser) {
                    selectedUserId.value = firstUser.id;
                }
            } else if (!selectedUserId.value && users.value.length > 0) {
                const firstUser = users.value[0];
                if (firstUser) {
                    selectedUserId.value = firstUser.id;
                }
            }
        }
    } finally {
        isLoading.value = false;
    }
}

function handleAdminUpdated(userId: string, isAdmin: boolean) {
    const user = users.value.find((u) => u.id === userId);
    if (user) {
        user.isAdmin = isAdmin;
    }
}

onMounted(async () => {
    await loadUsers();
});
</script>
