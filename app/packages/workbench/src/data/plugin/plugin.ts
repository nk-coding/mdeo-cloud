import type { LanguageContributionPlugin, LanguageEditorPlugin, LanguagePlugin, Plugin } from "@mdeo/plugin";
import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";

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
 * A resolved version of the langauge plugin, where import of the editor plugin is already handled
 */
export interface WorkbenchLanguagePlugin extends Omit<LanguagePlugin, "editorPlugin"> {
     /**
     * Editor plugin for the language, if undefined no graphical editor will be provided
     */
    editorPlugin: WorkbenchLanguageEditorPlugin | undefined;
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

export interface WorkbenchLanguageEditorPlugin extends Omit<LanguageEditorPlugin, "import"> {
    /**
     * Container configuration for the GLSP/Sprotty container.
     * This configures the model elements, views, and other diagram features.
     */
    containerConfiguration: ContainerConfiguration;
}