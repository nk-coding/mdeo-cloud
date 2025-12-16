import type { LanguagePlugin } from "./languagePlugin";
import type { ServerContributionPlugin, ServerLanguagePlugin } from "./serverPlugin";

/**
 * Plugin interface for the workbench
 */
export interface Plugin {
    /**
     * The id of the plugin
     */
    id: string;
    /**
     * Language plugins provided by the plugin
     */
    languagePlugins: LanguagePlugin[];
    /**
     * Server contribution plugins provided by the plugin
     */
    serverContributionPlugins: ServerContributionPlugin[];
}
