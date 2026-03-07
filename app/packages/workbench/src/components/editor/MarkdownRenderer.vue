<template>
    <ScrollArea class="h-full w-full px-8 pt-4">
        <div class="max-w-none prose prose-slate" v-html="renderedResult.html"></div>

        <template v-for="embed in resolvedEmbeds" :key="embed.id">
            <Teleport :to="`#${embed.id}`" v-if="embed.mounted">
                <div class="my-6 rounded-lg border overflow-hidden">
                    <div class="flex items-center justify-between bg-muted/50 px-4 py-2 border-b">
                        <span class="text-sm font-medium text-muted-foreground truncate">
                            {{ embed.alt || embed.fileName }}
                        </span>
                        <button
                            class="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors px-2 py-1 rounded-md hover:bg-muted"
                            @click="openInNewTab(embed.uri)"
                            title="Open in new tab"
                        >
                            <ExternalLink class="size-3.5" />
                            <span>Open</span>
                        </button>
                    </div>
                    <div v-if="embed.error" class="flex items-center gap-3 px-4 py-6 text-sm text-destructive">
                        <AlertCircle class="size-5 shrink-0" />
                        <span>{{ embed.error }}</span>
                    </div>
                    <div v-else-if="embed.languagePlugin" class="h-[400px] relative">
                        <GraphicalEditor
                            :tab="{ fileUri: embed.uri, temporary: true }"
                            :language-plugin="<ResolvedWorkbenchLanguagePlugin>embed.languagePlugin"
                            :editable="false"
                            is-active
                        />
                    </div>
                    <div v-else class="flex items-center gap-3 px-4 py-6 text-sm text-muted-foreground">
                        <AlertCircle class="size-5 shrink-0" />
                        <span>No graphical editor available for this file type.</span>
                    </div>
                </div>
            </Teleport>
        </template>
    </ScrollArea>
</template>
<script setup lang="ts">
import { computed, inject, nextTick, ref, useId, watch } from "vue";
import { remark } from "remark";
import remarkGfm from "remark-gfm";
import remarkRehype from "remark-rehype";
import rehypeStringify from "rehype-stringify";
import { rehypeAddClasses } from "./rehypeTailwindStyles";
import { rehypeFileEmbed, type FileEmbed } from "./rehypeFileEmbed";
import { showError } from "@/lib/notifications";
import { Uri } from "vscode";
import { computedAsync } from "@vueuse/core";
import { ScrollArea } from "../ui/scroll-area";
import { createModelReference } from "@codingame/monaco-vscode-api/monaco";
import { workbenchStateKey } from "../workbench/util";
import { FileCategory, parseUri } from "@mdeo/language-common";
import { getFileExtension } from "@/data/filesystem/util";
import GraphicalEditor from "./GraphicalEditor.vue";
import { AlertCircle, ExternalLink } from "lucide-vue-next";
import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";

const props = defineProps<{
    uri: Uri;
}>();

const { languagePluginByExtension, monacoApi, project } = inject(workbenchStateKey)!;

/**
 * Resolved embed descriptor with all information needed
 * to render an embedded graphical editor or error state.
 */
interface ResolvedEmbed {
    /**
     * Unique DOM element ID for the Teleport target.
     */
    id: string;
    /**
     * Display label from markdown alt text.
     */
    alt: string;
    /**
     * Resolved VSCode URI for the embedded file.
     */
    uri: Uri;
    /**
     * Extracted file name from the path.
     */
    fileName: string;
    /**
     * The resolved language plugin, or undefined if unsupported.
     */
    languagePlugin: ResolvedWorkbenchLanguagePlugin | undefined;
    /**
     * Error message if the file could not be found or loaded.
     */
    error: string | undefined;
    /**
     * Whether the Teleport target div has been mounted in the DOM.
     */
    mounted: boolean;
}

const content = computedAsync(async () => {
    const reference = await createModelReference(props.uri);
    const res = reference.object.textEditorModel?.getValue() ?? "";
    reference.dispose();
    return res;
}, "");

const embeddableExtensions = computed(() => {
    const extensions = new Set<string>();
    for (const plugin of languagePluginByExtension.value.values()) {
        if (plugin.graphicalEditorPlugin != undefined && plugin.extension) {
            extensions.add(plugin.extension);
        }
    }
    return extensions;
});

const baseId = useId();

/**
 * Processes the markdown content through the remark/rehype pipeline.
 * Produces both the rendered HTML string and a list of file embed descriptors
 * for any image nodes referencing embeddable model files.
 */
const renderedResult = computed<{ html: string; embeds: FileEmbed[] }>(() => {
    if (!content.value) {
        return { html: "", embeds: [] };
    }

    try {
        const embeds: FileEmbed[] = [];
        let embedCounter = 0;

        const result = remark()
            .use(remarkGfm)
            .use(remarkRehype, { allowDangerousHtml: true })
            .use(
                rehypeFileEmbed,
                (embed) => embeds.push(embed),
                embeddableExtensions.value,
                () => `${baseId}-${embedCounter++}`
            )
            .use(rehypeAddClasses)
            .use(rehypeStringify, { allowDangerousHtml: true })
            .processSync(content.value);

        return { html: String(result), embeds };
    } catch (error) {
        showError("Error rendering markdown content.");
        return { html: `<pre>${content.value}</pre>`, embeds: [] };
    }
});

/**
 * Resolves a file embed source path to a full VSCode URI.
 *
 * - **Relative paths** (e.g. `result.m_gen`) are treated as execution result files
 *   and resolved relative to the current execution context.
 * - **Absolute paths** (e.g. `/files/model.m`) are treated as regular project files.
 *
 * @param src The raw `src` attribute from the markdown image node
 * @returns The resolved URI
 */
function resolveEmbedUri(src: string): Uri {
    if (src.startsWith("/")) {
        return Uri.file(`/${project.value!.id}/files${src}`);
    }

    const parsed = parseUri(props.uri);

    if (parsed.category !== FileCategory.ExecutionSummary) {
        throw new Error(`Unsupported markdown file URI category: ${parsed.category}`);
    }
    return Uri.joinPath(Uri.file(`/${parsed.projectId}/executions/${parsed.executionId}/files`), src);
}

/**
 * Checks whether a file exists by attempting to read a model reference.
 *
 * @param uri The URI to check
 * @returns `true` if the file exists and is readable
 */
async function checkFileExists(uri: Uri): Promise<boolean> {
    try {
        const reference = await createModelReference(uri);
        reference.dispose();
        return true;
    } catch {
        return false;
    }
}

const resolvedEmbeds = ref<ResolvedEmbed[]>([]);

watch(
    () => renderedResult.value.embeds,
    async (embeds) => {
        const resolved: ResolvedEmbed[] = [];

        for (const embed of embeds) {
            const uri = resolveEmbedUri(embed.src);
            const ext = getFileExtension(uri.path);
            const plugin = languagePluginByExtension.value.get(ext);
            const fileName = uri.path.split("/").pop() ?? embed.src;

            let error: string | undefined;

            if (!plugin?.graphicalEditorPlugin) {
                error = `No graphical editor available for "${ext}" files.`;
            } else {
                const exists = await checkFileExists(uri);
                if (!exists) {
                    error = `File not found: ${embed.src}`;
                }
            }

            resolved.push({
                id: embed.id,
                alt: embed.alt,
                uri,
                fileName,
                languagePlugin: plugin,
                error,
                mounted: false
            });
        }

        resolvedEmbeds.value = resolved;

        await nextTick();
        for (const embed of resolvedEmbeds.value) {
            embed.mounted = true;
        }
    },
    { immediate: true }
);

/**
 * Opens the given file URI in a new editor tab.
 *
 * @param uri The URI of the file to open
 */
function openInNewTab(uri: Uri): void {
    monacoApi.editorService.openEditor({
        resource: uri,
        options: { preserveFocus: false }
    });
}
</script>
