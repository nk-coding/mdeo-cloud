import type { IconNode } from "lucide-vue-next";
import type { languages } from "monaco-editor";

/**
 * Plugin configuration for a language.
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
    serverPlugin: LanguageServerPlugin;
    /**
     * Editor plugin for the language, if undefined no graphical editor will be provided
     */
    editorPlugin: LanguageEditorPlugin | undefined;
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
 * Plugin configuration for a graphical editor.
 */
export interface LanguageEditorPlugin {
    /**
     * Import which resolves to the container configuration for the GLSP/Sprotty container.
     */
    import: string;

    /**
     * URL to the CSS styles for this editor.
     * This should be a URL that can be loaded by the browser.
     */
    stylesUrl: string;
}

/**
 * Server plugin configuration for a language.
 */
export interface LanguageServerPlugin {
    /**
     * Import which resolves to the server plugin module.
     */
    import: string;
}
