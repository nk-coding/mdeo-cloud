<template>
    <div class="space-y-6">
        <div class="space-y-3">
            <h2 class="text-2xl font-semibold text-foreground">
                {{ user.username }}
            </h2>
            <div class="space-y-4">
                <div class="flex items-center justify-between">
                    <div class="space-y-0.5">
                        <label class="text-sm font-medium">Administrator</label>
                        <p class="text-xs text-muted-foreground">
                            Administrators have full access to all projects and system settings.
                        </p>
                    </div>
                    <Button
                        :variant="user.isAdmin ? 'default' : 'outline'"
                        size="sm"
                        :disabled="updatingAdmin"
                        @click="handleAdminToggle(!user.isAdmin)"
                    >
                        {{ user.isAdmin ? "Admin" : "User" }}
                    </Button>
                </div>
            </div>
        </div>

        <Separator />

        <div class="space-y-3">
            <h3 class="text-lg font-medium text-foreground">Owned Projects</h3>
            <div v-if="loadingProjects" class="text-sm text-muted-foreground">Loading projects...</div>
            <div v-else-if="projects.length === 0" class="text-sm text-muted-foreground">
                This user doesn't own any projects.
            </div>
            <div v-else class="space-y-2">
                <div
                    v-for="project in projects"
                    :key="project.id"
                    class="flex items-center gap-2 p-2 rounded-md border text-sm"
                >
                    <Folder class="size-4 text-muted-foreground" />
                    <span>{{ project.name }}</span>
                </div>
            </div>
        </div>
    </div>

    <AlertDialog v-model:open="showAdminDialog">
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>{{ user.isAdmin ? "Remove" : "Grant" }} Administrator Access</AlertDialogTitle>
                <AlertDialogDescription>
                    {{
                        user.isAdmin
                            ? `Are you sure you want to remove administrator access for "${user.username}"? They will lose access to system settings and all projects.`
                            : `Are you sure you want to grant administrator access to "${user.username}"? They will have full access to all projects and system settings.`
                    }}
                </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction :disabled="updatingAdmin" @click="confirmAdminChange">Confirm</AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { Folder } from "lucide-vue-next";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
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
import type { User, BackendApi } from "@/data/api/backendApi";
import type { Project } from "@/data/project/project";

const props = defineProps<{
    user: User;
    backendApi: BackendApi;
}>();

const emit = defineEmits<{
    adminUpdated: [userId: string, isAdmin: boolean];
}>();

const projects = ref<Project[]>([]);
const loadingProjects = ref(false);
const updatingAdmin = ref(false);
const showAdminDialog = ref(false);
const pendingAdminValue = ref(false);

async function loadProjects() {
    loadingProjects.value = true;
    try {
        const result = await props.backendApi.users.getProjects(props.user.id);
        if (result.success) {
            projects.value = result.value;
        }
    } finally {
        loadingProjects.value = false;
    }
}

function handleAdminToggle(newValue: boolean) {
    pendingAdminValue.value = newValue;
    showAdminDialog.value = true;
}

async function confirmAdminChange() {
    updatingAdmin.value = true;
    try {
        const result = await props.backendApi.users.updateAdmin(props.user.id, pendingAdminValue.value);
        if (result.success) {
            emit("adminUpdated", props.user.id, pendingAdminValue.value);
        }
    } finally {
        updatingAdmin.value = false;
        showAdminDialog.value = false;
    }
}

watch(
    () => props.user.id,
    () => {
        loadProjects();
    },
    { immediate: true }
);
</script>
