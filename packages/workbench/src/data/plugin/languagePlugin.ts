import type { ServerLanguagePlugin } from "./serverPlugin.js";

/**
 * A plugin for a language handled by the workbench
 */
export interface LanguagePlugin {
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
}
