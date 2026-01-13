import type { IconNode } from "lucide-vue-next";
import type { LanguageContributionPlugin } from "./languageContributionPlugin.js";
import type { LanguagePlugin } from "./languagePlugin.js";

/**
 * Plugin interface for the mdeo platform
 */
export interface Plugin {
    /**
     * Unique identifier for the plugin
     */
    id: string;

    /**
     * URL where the plugin is hosted
     */
    url: string;

    /**
     * Display name of the plugin
     */
    name: string;

    /**
     * Description of the plugin
     */
    description: string;

    /**
     * Icon representing the plugin (from Lucide)
     */
    icon: IconNode;

    /**
     * Language plugins provided by the plugin
     */
    languagePlugins: LanguagePlugin[];

    /**
     * Language contribution plugins provided by the plugin
     */
    contributionPlugins: LanguageContributionPlugin[];
}