import type { WorkbenchLanguagePlugin } from "./languagePlugin";
import type { ServerContributionPlugin } from "./serverPlugin";

/**
 * Plugin interface for the workbench
 */
export interface WorkbenchPlugin {
    /**
     * The id of the plugin
     */
    id: string;
    /**
     * Language plugins provided by the plugin
     */
    languagePlugins: WorkbenchLanguagePlugin[];
    /**
     * Server contribution plugins provided by the plugin
     */
    serverContributionPlugins: ServerContributionPlugin[];
}
