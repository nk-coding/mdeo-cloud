<template>
    <TreeItem :data="temporaryItem" :mode="'edit'" :is-folder="itemType === 'folder'" :has-children="false" @click.stop>
        <template #content>
            <FileIcon v-if="itemType === 'file'" :is="FileIcon" class="w-4 h-4" />
            <span class="flex flex-1">
                <TreeItemInput
                    :model-value="fileName"
                    :validate="validateFileName"
                    @update:model-value="updateFileName"
                    @submit="handleSubmit"
                    @cancel="handleCancel"
                />{{ fileExtension }}
            </span>
        </template>
    </TreeItem>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { File as FileIcon } from "lucide-vue-next";
import TreeItem from "@/components/tree/TreeItem.vue";
import TreeItemInput from "../tree/TreeItemInput.vue";
import type { Folder } from "@/data/filesystem/file";
import type { FileTypePlugin } from "@/data/plugin/fileTypePlugin";

const props = defineProps<{
    itemType: "file" | "folder";
    parent: Folder;
    fileType?: FileTypePlugin;
}>();

const emit = defineEmits<{
    submit: [name: string, itemType: "file" | "folder", parentId: string, fileType?: FileTypePlugin];
    cancel: [];
}>();

const fileName = ref("");
const submitted = ref(false);

const temporaryItem = computed(() => ({
    id: `new-${props.itemType}`
}));

const fileExtension = computed(() => {
    if (props.itemType === "file" && props.fileType?.extension) {
        return props.fileType.extension;
    }
    return "";
});

function updateFileName(value: string) {
    fileName.value = value;
}

function handleSubmit(value: string) {
    if (submitted.value) {
        return;
    }
    if (value.trim()) {
        const fullName =
            props.itemType === "file" && fileExtension.value ? `${value.trim()}${fileExtension.value}` : value.trim();
        submitted.value = true;
        emit("submit", fullName, props.itemType, props.parent.id, props.fileType);
    } else {
        handleCancel();
    }
}

function handleCancel() {
    if (submitted.value) {
        return;
    }
    submitted.value = true;
    emit("cancel");
}

function validateFileName(name: string) {
    if (name.trim().length === 0) {
        return false;
    }
    const baseName = name.trim();

    const fullName = props.itemType === "file" && fileExtension.value ? `${baseName}${fileExtension.value}` : baseName;
    const duplicate = props.parent.children.find((child) => child.name === fullName);
    if (duplicate) {
        return false;
    }
    return true;
}
</script>
