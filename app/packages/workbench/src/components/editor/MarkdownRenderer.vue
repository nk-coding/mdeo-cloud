<template>
    <ScrollArea class="h-full w-full px-8 pt-4">
        <div class="max-w-none prose prose-slate" v-html="renderedHtml"></div>
    </ScrollArea>
</template>
<script setup lang="ts">
import { computed } from "vue";
import { remark } from "remark";
import remarkGfm from "remark-gfm";
import remarkRehype from "remark-rehype";
import rehypeStringify from "rehype-stringify";
import { rehypeAddClasses } from "./rehypeTailwindStyles";
import { showError } from "@/lib/notifications";
import type { Uri } from "vscode";
import { computedAsync } from "@vueuse/core";
import { ScrollArea } from "../ui/scroll-area";
import { createModelReference } from "@codingame/monaco-vscode-api/monaco";

const props = defineProps<{
    uri: Uri;
}>();

const content = computedAsync(async () => {
    const reference = await createModelReference(props.uri);
    const res = reference.object.textEditorModel?.getValue() ?? "";
    reference.dispose();
    return res;
}, "");

const renderedHtml = computed(() => {
    try {
        const result = remark()
            .use(remarkGfm)
            .use(remarkRehype, { allowDangerousHtml: true })
            .use(rehypeAddClasses)
            .use(rehypeStringify, { allowDangerousHtml: true })
            .processSync(content.value);
        return String(result);
    } catch (error) {
        showError("Error rendering markdown content.");
        return `<pre>${content.value}</pre>`;
    }
});
</script>