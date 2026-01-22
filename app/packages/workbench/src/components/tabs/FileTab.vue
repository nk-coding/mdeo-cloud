<template>
    <div class="group relative">
        <ContextMenu>
            <ContextMenuTrigger as-child>
                <TabsTrigger
                    :value="`file:${tab.fileUri.toString()}`"
                    :class="
                        cn(
                            'data-[state=active]:bg-accent hover:bg-accent/75 text-foreground',
                            'inline-flex h-[calc(100%-1px)] items-center justify-center gap-1.5 rounded-md border border-transparent pl-2 pr-1 py-1 text-sm whitespace-nowrap transition-[color,box-shadow]',
                            'group/tab',
                            'scroll-m-2'
                        )
                    "
                    @dblclick="tab.temporary = false"
                    @click.middle="handleClose"
                >
                    <FileTypeIcon
                        :model-value="languagePluginByExtension.get(getFileExtension(tab.fileUri.path))"
                        class="size-4"
                    />
                    <span class="truncate max-w-64" :class="{ italic: tab.temporary }">{{ fileName }}</span>
                    <span
                        @click.stop="handleClose"
                        class="shrink-0 p-0.5 rounded hover:bg-muted-foreground/20 transition-opacity flex items-center justify-center size-4 invisible group-hover/tab:visible group-data-[state=active]/tab:visible"
                        role="button"
                        tabindex="0"
                        @keydown.enter.stop="handleClose"
                        @keydown.space.stop="handleClose"
                    >
                        <X class="size-3" />
                    </span>
                </TabsTrigger>
            </ContextMenuTrigger>
            <ContextMenuContent>
                <ContextMenuItem @click="handleClose"> Close </ContextMenuItem>
                <ContextMenuItem @click="emit('closeOthers', tab)"> Close Others </ContextMenuItem>
                <ContextMenuItem @click="emit('closeToRight', tab)"> Close to the Right </ContextMenuItem>
                <ContextMenuSeparator />
                <ContextMenuItem @click="emit('closeAll')"> Close All </ContextMenuItem>
            </ContextMenuContent>
        </ContextMenu>
    </div>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";
import { TabsTrigger } from "reka-ui";
import type { EditorTab } from "@/data/tab/editorTab";
import { X } from "lucide-vue-next";
import { cn } from "@/lib/utils";
import FileTypeIcon from "../FileTypeIcon.vue";
import { workbenchStateKey } from "../workbench/util";
import {
    ContextMenu,
    ContextMenuTrigger,
    ContextMenuContent,
    ContextMenuItem,
    ContextMenuSeparator
} from "@/components/ui/context-menu";
import { getFileExtension } from "@/data/filesystem/util";

const props = defineProps<{
    tab: EditorTab;
}>();

const { languagePluginByExtension } = inject(workbenchStateKey)!;

const emit = defineEmits<{
    close: [tab: EditorTab];
    closeOthers: [tab: EditorTab];
    closeToRight: [tab: EditorTab];
    closeAll: [];
}>();

const fileName = computed(() => {
    return props.tab.fileUri.path.split("/").pop() ?? props.tab.fileUri.path;
});

function handleClose() {
    emit("close", props.tab);
}
</script>
