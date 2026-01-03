<template>
    <div class="group relative">
        <TabsTrigger
            :value="tab.file.id.toString()"
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
            <FileTypeIcon :model-value="languagePluginByExtension.get(tab.file.extension)" class="w-4 h-4" />
            <span class="truncate max-w-64" :class="{ italic: tab.temporary }">{{ fileName }}</span>
            <span
                @click.stop="handleClose"
                class="flex-shrink-0 p-0.5 rounded hover:bg-muted-foreground/20 transition-opacity flex items-center justify-center size-4 invisible group-hover/tab:visible group-data-[state=active]/tab:visible"
                role="button"
                tabindex="0"
                @keydown.enter.stop="handleClose"
                @keydown.space.stop="handleClose"
            >
                <X class="h-3 w-3" />
            </span>
        </TabsTrigger>
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

interface Props {
    tab: EditorTab;
}

const props = defineProps<Props>();

const { languagePluginByExtension } = inject(workbenchStateKey)!;

const emit = defineEmits<{
    close: [tab: EditorTab];
}>();

const fileName = computed(() => {
    const path = props.tab.file.id;
    return path.path.split("/").pop() ?? path.path;
});

function handleClose() {
    emit("close", props.tab);
}
</script>
