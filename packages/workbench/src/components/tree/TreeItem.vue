<template>
    <TreeButton
        v-if="!isFolder"
        v-bind="$attrs"
        :data="data"
        :mode="mode"
        @dragenter="handleDragEnter"
        @dragleave="handleDragLeave"
        @dragover="handleDragOver"
        @drop="handleDrop"
        @click="handleFileClick"
        @dblclick="handleFileDoubleClick"
        @keydown.enter="handleFileEnter"
    >
        <slot name="content" />
    </TreeButton>

    <li
        v-else
        data-slot="tree-item"
        data-tree="item"
        :class="cn('group/tree-item relative')"
        @dragenter="handleDragEnter"
        @dragleave="handleDragLeave"
        @dragover="handleDragOver"
        @drop="handleDrop"
    >
        <Collapsible
            v-model:open="isOpen"
            class="group/collapsible [&[data-state=open]>button>svg:first-child]:rotate-90"
        >
            <CollapsibleTrigger as-child>
                <TreeButton v-bind="$attrs" :data="data" :mode="mode">
                    <ChevronRight class="transition-transform" />
                    <slot name="content" />
                </TreeButton>
            </CollapsibleTrigger>
            <CollapsibleContent v-if="hasChildren">
                <ul
                    data-slot="tree-sub"
                    data-tree="sub"
                    :class="
                        cn(
                            'border-sidebar-border ml-3.5 mr-px flex min-w-0 translate-x-px flex-col gap-1 border-l pl-2.5 py-0.5',
                            'group-data-[collapsible=icon]:hidden'
                        )
                    "
                    @click.stop
                >
                    <slot name="items" />
                </ul>
            </CollapsibleContent>
        </Collapsible>
    </li>
</template>

<script setup lang="ts" generic="T extends TreeItem">
import { ChevronRight } from "lucide-vue-next";
import { cn } from "@/lib/utils";

import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import TreeButton, { type TreeButtonProps } from "./TreeButton.vue";
import { dragAndDropKey, type TreeItem } from "./util";
import { computed, inject, ref } from "vue";
import { workbenchStateKey } from "@/data/workbenchState";
import { FileType, type File } from "@/data/filesystem/file";

const props = defineProps<
    {
        data: T;
        isFolder: boolean;
        hasChildren: boolean;
        forceOpen?: boolean;
    } & TreeButtonProps
>();

const _isOpen = ref(false);
const hoverTimeout = ref<number | null>(null);

const dragAndDropConfig = inject(dragAndDropKey);
const workbenchState = inject(workbenchStateKey);

const isOpen = computed({
    get() {
        return props.forceOpen || _isOpen.value;
    },
    set(value: boolean) {
        if (props.forceOpen != true) {
            _isOpen.value = value;
        }
    }
});

function handleDragEnter(event: DragEvent) {
    if (!dragAndDropConfig?.value.enabled || !props.isFolder) {
        return;
    }

    event.preventDefault();

    if (hoverTimeout.value) {
        clearTimeout(hoverTimeout.value);
    }
    hoverTimeout.value = setTimeout(() => {
        if (!isOpen.value) {
            isOpen.value = true;
        }
    }, 1000);
}

function handleDragLeave(event: DragEvent) {
    if (!dragAndDropConfig?.value.enabled) {
        return;
    }

    if (hoverTimeout.value) {
        clearTimeout(hoverTimeout.value);
        hoverTimeout.value = null;
    }
}

function handleDragOver(event: DragEvent) {
    if (!dragAndDropConfig?.value.enabled) {
        return;
    }

    event.preventDefault();
}

function handleDrop(event: DragEvent) {
    if (!dragAndDropConfig?.value.enabled) {
        return;
    }

    event.preventDefault();

    if (hoverTimeout.value) {
        clearTimeout(hoverTimeout.value);
        hoverTimeout.value = null;
    }

    const draggedItemData = event.dataTransfer?.getData("application/json");
    if (!draggedItemData) {
        return;
    }

    const draggedItemInfo = JSON.parse(draggedItemData);

    const draggedItem = { id: draggedItemInfo.id };

    const canDrop = dragAndDropConfig.value.callbacks?.canDrop?.(draggedItem, props.data) ?? true;
    if (!canDrop) {
        return;
    }

    if (props.isFolder) {
        event.stopPropagation();
        dragAndDropConfig.value.callbacks?.onDrop?.(draggedItem, props.data, event);
    }
}

function openTab(temporary: boolean, event?: MouseEvent | KeyboardEvent) {
    if (!props.isFolder && props.mode !== 'edit') {
        const fileData = props.data as any;
        
        if (fileData.type !== FileType.FILE || !workbenchState) {
            return;
        }

        if (event instanceof KeyboardEvent) {
            event.preventDefault();
        }

        const file = fileData as File;
        
        // Check if tab already exists
        const existingTab = workbenchState.value.tabs.value.find(tab => tab.file.path === file.path);
        
        if (existingTab) {
            // Tab already exists, activate it
            workbenchState.value.activeTab.value = existingTab;
            // If opening as permanent, update the tab
            if (!temporary && existingTab.temporary) {
                existingTab.temporary = false;
            }
        } else {
            // Create new tab
            const newTab = {
                file: file,
                temporary: temporary
            };
            workbenchState.value.tabs.value.push(newTab);
            workbenchState.value.activeTab.value = newTab;
        }
    }
}

const handleFileClick = (event: MouseEvent) => openTab(true, event);
const handleFileDoubleClick = (event: MouseEvent) => openTab(false, event);
const handleFileEnter = (event: KeyboardEvent) => openTab(false, event);
</script>
