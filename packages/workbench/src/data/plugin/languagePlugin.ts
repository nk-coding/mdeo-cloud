import type { languages } from "monaco-editor";
import type { ServerContributionPlugin, ServerLanguagePlugin } from "./serverPlugin.js";
import type { IconNode } from "lucide-vue-next";
import type { EditorPlugin } from "@mdeo/editor-shared";

/**
 * A plugin for a language handled by the workbench
 */
export interface WorkbenchLanguagePlugin {
    /**
     * Unique identifier for the language
     */
    id: string;
    /**
     * The name of the language, displayed in the UI
     */
    name: string;
    /**
     * The file extension associated with the language (including the dot)
     */
    extension: string;
    /**
     * Optional default content for new files of this type
     */
    defaultContent?: string;
    /**
     * Server plugin for the language
     */
    serverPlugin: Omit<ServerLanguagePlugin, "type" | "languageId" | "extension">;
    /**
     * Editor plugin for the language, if undefined no graphical editor will be provided
     */
    editorPlugin: EditorPlugin | undefined;
    /**
     * Configuration for the language in the editor
     */
    languageConfiguration: languages.LanguageConfiguration;
    /**
     * Monarch tokens provider for syntax highlighting in the editor
     */
    monarchTokensProvider: languages.IMonarchLanguage;
    /**
     * Icon representing the language
     */
    icon: IconNode;
}

/**
 * A language plugin that has been resolved with its associated server contribution plugins
 */
export interface ResolvedLanguagePlugin extends WorkbenchLanguagePlugin {
    /**
     * The server contribution plugins associated with this language plugin
     */
    serverContributionPlugins: ServerContributionPlugin[];
}
