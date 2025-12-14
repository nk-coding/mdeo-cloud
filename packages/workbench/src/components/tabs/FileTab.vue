<template>
    <div class="group relative">
        <TabsTrigger
            :value="tab.file.path"
            :class="
                cn(
                    'data-[state=active]:bg-accent hover:bg-accent/75 text-foreground cursor-pointer',
                    'inline-flex h-[calc(100%-1px)] items-center justify-center gap-1.5 rounded-md border border-transparent px-2 py-1 text-sm whitespace-nowrap transition-[color,box-shadow]',
                    'group/tab'
                )
            "
            @dblclick="tab.temporary = false"
        >
            <span class="truncate max-w-64" :class="{ italic: tab.temporary }">{{ fileName }}</span>
            <span
                @click="handleClose"
                class="flex-shrink-0 p-0.5 rounded hover:bg-muted-foreground/20 transition-opacity cursor-pointer flex items-center justify-center size-4 invisible group-hover/tab:visible group-data-[state=active]/tab:visible"
                role="button"
                tabindex="0"
                @keydown.enter="handleClose"
                @keydown.space="handleClose"
            >
                <X class="h-3 w-3" />
            </span>
        </TabsTrigger>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { TabsTrigger } from "reka-ui";
import type { EditorTab } from "@/data/tab/editorTab";
import { X } from "lucide-vue-next";
import { cn } from "@/lib/utils";

interface Props {
    tab: EditorTab;
}

const props = defineProps<Props>();
const emit = defineEmits<{
    close: [tab: EditorTab];
}>();

const fileName = computed(() => {
    const path = props.tab.file.path;
    return path.split("/").pop() || path;
});

const handleClose = (e: Event) => {
    e.stopPropagation();
    emit("close", props.tab);
};
</script>
