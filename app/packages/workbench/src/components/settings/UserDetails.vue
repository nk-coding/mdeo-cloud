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
                    <Switch v-model="isAdmin" :disabled="isUpdating || disableAdminToggle" />
                </div>
                <div class="flex items-center justify-between">
                    <div class="space-y-0.5">
                        <label class="text-sm font-medium">Create Projects</label>
                        <p class="text-xs text-muted-foreground">Allows creating new projects globally.</p>
                    </div>
                    <Switch v-model="canCreateProject" :disabled="isUpdating || isAdmin" />
                </div>
            </div>
        </div>

        <Separator />

        <div class="space-y-3">
            <h3 class="text-lg font-medium text-foreground">Projects</h3>
            <div v-if="loadingProjects" class="text-sm text-muted-foreground">Loading projects...</div>
            <div v-else-if="projects.length === 0" class="text-sm text-muted-foreground">
                This user is not a member of any projects.
            </div>
            <div v-else class="space-y-2">
                <div
                    v-for="project in projects"
                    :key="project.projectId"
                    class="flex items-center gap-2 p-2 rounded-md border text-sm"
                >
                    <Folder class="size-4 text-muted-foreground" />
                    <span class="truncate">{{ project.projectName }}</span>
                    <span
                        v-if="project.isAdmin"
                        class="ml-auto text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground font-medium"
                        >Admin</span
                    >
                    <span
                        v-if="!project.isAdmin && project.canExecute"
                        class="text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground font-medium"
                        >Execute</span
                    >
                    <span
                        v-if="!project.isAdmin && project.canWrite"
                        class="text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground font-medium"
                        >Write</span
                    >
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { Folder } from "lucide-vue-next";
import { Separator } from "@/components/ui/separator";
import { Switch } from "@/components/ui/switch";
import type { User, BackendApi } from "@/data/api/backendApi";
import type { UserProjectMembership } from "@/data/api/backendApi";
import { showApiError } from "@/lib/notifications";

const props = defineProps<{
    user: User;
    backendApi: BackendApi;
    disableAdminToggle: boolean;
}>();

const emit = defineEmits<{
    permissionsUpdated: [userId: string, isAdmin: boolean, canCreateProject: boolean];
}>();

const projects = ref<UserProjectMembership[]>([]);
const loadingProjects = ref(false);
const isUpdating = ref(false);
const isAdmin = ref(false);
const canCreateProject = ref(false);
const isSyncingFromProps = ref(false);
const isApplyingPermissionChange = ref(false);

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

async function savePermissions() {
    isUpdating.value = true;
    try {
        const result = await props.backendApi.users.updatePermissions(
            props.user.id,
            isAdmin.value,
            canCreateProject.value
        );
        if (result.success) {
            emit("permissionsUpdated", props.user.id, isAdmin.value, canCreateProject.value);
        } else {
            showApiError("update user permissions", result.error.message);
            isAdmin.value = props.user.isAdmin;
            canCreateProject.value = props.user.canCreateProject;
        }
    } finally {
        isUpdating.value = false;
    }
}

async function handleAdminChange(value: boolean) {
    isApplyingPermissionChange.value = true;
    try {
        isAdmin.value = value;
        if (value) {
            canCreateProject.value = true;
        }
        await savePermissions();
    } finally {
        isApplyingPermissionChange.value = false;
    }
}

async function handleCreateProjectChange(value: boolean) {
    isApplyingPermissionChange.value = true;
    try {
        canCreateProject.value = value;
        await savePermissions();
    } finally {
        isApplyingPermissionChange.value = false;
    }
}

function syncLocalPermissionsFromProps() {
    isSyncingFromProps.value = true;
    isAdmin.value = props.user.isAdmin;
    canCreateProject.value = props.user.canCreateProject;
    isSyncingFromProps.value = false;
}

watch(
    () => props.user.id,
    () => {
        syncLocalPermissionsFromProps();
        loadProjects();
    },
    { immediate: true }
);

watch(
    () => [props.user.isAdmin, props.user.canCreateProject],
    ([nextIsAdmin, nextCanCreateProject]) => {
        if (!isUpdating.value && !isSyncingFromProps.value) {
            isAdmin.value = nextIsAdmin ?? false;
            canCreateProject.value = nextCanCreateProject ?? false;
        }
    }
);

watch(isAdmin, async (nextValue, previousValue) => {
    if (
        isSyncingFromProps.value ||
        isUpdating.value ||
        isApplyingPermissionChange.value ||
        nextValue === previousValue
    ) {
        return;
    }
    await handleAdminChange(nextValue);
});

watch(canCreateProject, async (nextValue, previousValue) => {
    if (
        isSyncingFromProps.value ||
        isUpdating.value ||
        isApplyingPermissionChange.value ||
        nextValue === previousValue
    ) {
        return;
    }
    await handleCreateProjectChange(nextValue);
});
</script>
