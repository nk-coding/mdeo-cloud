<template>
    <Field class="w-full">
        <FieldLabel v-if="label">{{ label }}</FieldLabel>
        <FieldContent>
            <div class="relative">
                <PopoverRoot v-model:open="open" @update:open="handleOpenChange">
                    <PopoverTrigger as-child>
                        <button
                            ref="triggerRef"
                            type="button"
                            :class="
                                cn(
                                    'border-input [&_svg:not([class*=\'text-\'])]:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive dark:bg-input/30 dark:hover:bg-input/50 flex w-full items-center justify-between gap-2 rounded-md border bg-transparent px-3 py-2 text-sm whitespace-nowrap shadow-xs transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50 h-9',
                                    !model && 'text-muted-foreground'
                                )
                            "
                            :aria-describedby="fieldErrors.length > 0 ? errorId : undefined"
                        >
                            <span class="truncate">{{
                                model
                                    ? displayValue
                                    : (schema.placeholder ??
                                      (schema.selectDirectory ? "Select a folder" : "Select a file"))
                            }}</span>
                            <ChevronDown class="size-4 opacity-50 shrink-0" />
                        </button>
                    </PopoverTrigger>

                    <PopoverPortal>
                        <PopoverContent
                            :side-offset="4"
                            align="start"
                            :avoid-collisions="false"
                            :collision-padding="20"
                            :class="
                                cn(
                                    'z-50',
                                    'flex flex-col',
                                    'data-[state=open]:animate-in data-[state=closed]:animate-out',
                                    'data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
                                    'data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95',
                                    'data-[side=bottom]:slide-in-from-top-2',
                                    'w-(--reka-popper-anchor-width)',
                                    'h-[var(--reka-popover-content-available-height)]'
                                )
                            "
                            @escape-key-down="handleEscapeKey"
                        >
                            <div
                                class="bg-popover text-popover-foreground flex flex-initial w-full flex-col overflow-hidden rounded-md shadow-md border"
                            >
                                <ScrollArea class="flex-initial min-h-0 w-full">
                                    <Tree class="p-1" :active-element="pendingNode" :expanded-items="expandedItems">
                                        <FileSelectTreeItemList
                                            :nodes="treeData"
                                            :select-directory="schema.selectDirectory ?? false"
                                            @select="handleTreeSelect"
                                            @dblclick-select="handleTreeDoubleClick"
                                        />
                                    </Tree>
                                </ScrollArea>
                                <div v-if="schema.selectDirectory" class="border-t px-2 py-1.5 flex justify-end gap-2">
                                    <Button variant="ghost" size="sm" @click="open = false">Cancel</Button>
                                    <Button size="sm" :disabled="!pendingNode" @click="confirmSelection">OK</Button>
                                </div>
                            </div>
                        </PopoverContent>
                    </PopoverPortal>
                </PopoverRoot>
            </div>
            <FieldError v-if="fieldErrors.length > 0" :id="errorId" :errors="fieldErrors.map((e) => e.message)" />
        </FieldContent>
    </Field>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type {
    ActionSchemaFileSelectForm,
    ActionSchemaFileSelectNode,
    ActionValidationError
} from "@mdeo/language-common";
import { Field, FieldContent, FieldLabel, FieldError } from "@/components/ui/field";
import { Button } from "@/components/ui/button";
import ScrollArea from "@/components/ui/scroll-area/ScrollArea.vue";
import Tree from "@/components/tree/Tree.vue";
import FileSelectTreeItemList from "./FileSelectTreeItemList.vue";
import { PopoverRoot, PopoverTrigger, PopoverContent, PopoverPortal } from "reka-ui";
import { ChevronDown } from "lucide-vue-next";
import { cn } from "@/lib/utils";
import { getErrorsForPath } from "./actionFormUtils";

export interface FileSelectTreeNode {
    id: string;
    name: string;
    isFolder: boolean;
    children: FileSelectTreeNode[];
    parent?: FileSelectTreeNode;
}

const props = defineProps<{
    schema: ActionSchemaFileSelectForm;
    errors: ActionValidationError[];
    path: string;
    label?: string;
}>();

const model = defineModel<string>();

const selectId = computed(() => `file-select-${props.path.replace(/\//g, "-")}`);
const errorId = computed(() => `${selectId.value}-error`);
const fieldErrors = computed(() => getErrorsForPath(props.errors, props.path));

const open = ref(false);
const pendingNode = ref<FileSelectTreeNode | undefined>();
const expandedItems = ref<Set<FileSelectTreeNode>>(new Set());

const treeData = computed(() => convertNodes(props.schema.fileSelect, undefined));

function convertNodes(
    nodes: ActionSchemaFileSelectNode[],
    parent: FileSelectTreeNode | undefined
): FileSelectTreeNode[] {
    return nodes.map((n) => convertNode(n, parent));
}

function convertNode(node: ActionSchemaFileSelectNode, parent: FileSelectTreeNode | undefined): FileSelectTreeNode {
    const isFolder = node.children !== undefined && node.children.length > 0;
    const treeNode: FileSelectTreeNode = {
        id: parent ? `${parent.id}/${node.name}` : node.name,
        name: node.name,
        isFolder,
        children: [],
        parent
    };
    treeNode.children = node.children ? convertNodes(node.children, treeNode) : [];
    return treeNode;
}

function getAbsolutePath(node: FileSelectTreeNode): string {
    const parts: string[] = [];
    let current: FileSelectTreeNode | undefined = node;
    while (current != undefined) {
        parts.unshift(current.name);
        current = current.parent;
    }
    return props.schema.rootPath + "/" + parts.join("/");
}

const displayValue = computed(() => {
    if (model.value == undefined) {
        return "";
    }
    const rootPath = props.schema.rootPath;
    const relative = model.value.startsWith(rootPath + "/") ? model.value.slice(rootPath.length + 1) : model.value;
    return relative || model.value;
});

watch(open, (isOpen) => {
    if (isOpen) {
        pendingNode.value = model.value != undefined ? findNodeByPath(treeData.value, model.value) : undefined;
        expandedItems.value = new Set();
        if (pendingNode.value != undefined) {
            expandParentsOf(pendingNode.value);
        }
    }
});

function findNodeByPath(nodes: FileSelectTreeNode[], absPath: string): FileSelectTreeNode | undefined {
    for (const node of nodes) {
        if (getAbsolutePath(node) === absPath) {
            return node;
        }
        if (node.children.length > 0) {
            const found = findNodeByPath(node.children, absPath);
            if (found) {
                return found;
            }
        }
    }
    return undefined;
}

function expandParentsOf(node: FileSelectTreeNode): void {
    let current = node.parent;
    while (current != undefined) {
        expandedItems.value.add(current);
        current = current.parent;
    }
}

function handleOpenChange(isOpen: boolean) {
    if (!isOpen && pendingNode.value) {
        commitSelection();
    }
}

function handleTreeSelect(node: FileSelectTreeNode) {
    pendingNode.value = node;
    if (!props.schema.selectDirectory) {
        confirmSelection();
    }
}

function handleTreeDoubleClick(node: FileSelectTreeNode) {
    pendingNode.value = node;
    confirmSelection();
}

function handleEscapeKey() {
    pendingNode.value = undefined;
}

function commitSelection() {
    if (pendingNode.value) {
        model.value = getAbsolutePath(pendingNode.value);
    }
}

function confirmSelection() {
    commitSelection();
    open.value = false;
}
</script>
