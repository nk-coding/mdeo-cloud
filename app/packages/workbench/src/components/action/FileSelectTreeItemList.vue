<template>
    <template v-for="node in sortedNodes" :key="node.id">
        <TreeItem
            :data="node"
            :is-folder="node.isFolder"
            :has-children="node.isFolder && node.children.length > 0"
            :mode="isSelectable(node) ? 'default' : 'non-selectable'"
            @click="handleClick(node)"
            @dblclick="handleDoubleClick(node, $event)"
        >
            <template #content>
                <FileTypeIcon
                    v-if="!node.isFolder"
                    :model-value="languagePluginByExtension.get(getFileExtension(node.name))"
                    class="size-4 shrink-0"
                />
                <span>{{ node.name }}</span>
            </template>
            <template v-if="node.isFolder && node.children.length > 0" #items>
                <FileSelectTreeItemList
                    :nodes="node.children"
                    :select-directory="selectDirectory"
                    @select="$emit('select', $event)"
                    @dblclick-select="$emit('dblclick-select', $event)"
                />
            </template>
        </TreeItem>
    </template>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";
import TreeItem from "@/components/tree/TreeItem.vue";
import FileTypeIcon from "@/components/FileTypeIcon.vue";
import { getFileExtension } from "@/data/filesystem/util";
import { workbenchStateKey } from "@/components/workbench/util";
import type { FileSelectTreeNode } from "./ActionFileSelectForm.vue";

const { languagePluginByExtension } = inject(workbenchStateKey)!;

const props = defineProps<{
    nodes: FileSelectTreeNode[];
    selectDirectory: boolean;
}>();

const emit = defineEmits<{
    select: [node: FileSelectTreeNode];
    "dblclick-select": [node: FileSelectTreeNode];
}>();

const sortedNodes = computed(() => {
    return [...props.nodes].sort((a, b) => {
        if (a.isFolder !== b.isFolder) return a.isFolder ? -1 : 1;
        return a.name.localeCompare(b.name);
    });
});

function isSelectable(node: FileSelectTreeNode): boolean {
    if (props.selectDirectory) {
        return node.isFolder;
    }
    return !node.isFolder;
}

function handleClick(node: FileSelectTreeNode) {
    if (isSelectable(node)) {
        emit("select", node);
    }
}

function handleDoubleClick(node: FileSelectTreeNode, event: MouseEvent) {
    if (isSelectable(node)) {
        event.stopPropagation();
        emit("dblclick-select", node);
    }
}
</script>
