import type {
    LanguageContributionPlugin,
    LanguageGraphicalEditorPlugin,
    LanguagePlugin,
    LanguageTextualEditorPlugin,
    Plugin
} from "@mdeo/plugin";
import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import type { languages } from "monaco-editor";

/**
 * An extension to the base plugin interface for workbench-specific functionality.
 */
export interface WorkbenchPlugin extends Omit<Plugin, "languagePlugins"> {
    /**
     * Language plugins provided by the workbench plugin
     */
    languagePlugins: WorkbenchLanguagePlugin[];
}

/**
 * A resolved version of the language plugin, where import of the editor plugin is already handled
 * and monarchTokensProvider is deserialized to contain actual RegExp objects.
 */
export interface WorkbenchLanguagePlugin extends Omit<LanguagePlugin, "graphicalEditorPlugin" | "textualEditorPlugin"> {
    /**
     * Graphical editor plugin for the language, if undefined no graphical editor will be provided
     */
    graphicalEditorPlugin: WorkbenchLanguageGraphicalEditorPlugin | undefined;

    /**
     * Textual editor plugin for the language, if undefined no textual editor will be provided.
     * Contains deserialized monarch tokens provider with actual RegExp objects.
     */
    textualEditorPlugin: WorkbenchLanguageTextualEditorPlugin | undefined;
}

/**
 * An extension to the resolved language plugin with the associated contribution plugins
 */
export interface ResolvedWorkbenchLanguagePlugin extends WorkbenchLanguagePlugin {
    /**
     * Language contribution plugins associated with this language plugin
     */
    contributionPlugins: LanguageContributionPlugin[];
}

/**
 * Workbench-specific graphical editor plugin with resolved container configuration.
 */
export interface WorkbenchLanguageGraphicalEditorPlugin extends Omit<LanguageGraphicalEditorPlugin, "import"> {
    /**
     * Container configuration for the GLSP/Sprotty container.
     * This configures the model elements, views, and other diagram features.
     */
    containerConfiguration: ContainerConfiguration;
}

/**
 * Workbench-specific textual editor plugin with deserialized monarch tokens.
 */
export interface WorkbenchLanguageTextualEditorPlugin extends Omit<
    LanguageTextualEditorPlugin,
    "monarchTokensProvider"
> {
    /**
     * Monarch tokens provider for syntax highlighting in the editor (deserialized with RegExp objects)
     */
    monarchTokensProvider: languages.IMonarchLanguage;
}
