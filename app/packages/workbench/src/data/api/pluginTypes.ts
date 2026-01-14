import type { IconNode } from "lucide-vue-next";

/**
 * Represents a plugin that can be installed and used in the workbench.
 */
export interface BackendPlugin {
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
     * Language plugins provided by this plugin
     */
    languagePlugins: BackendLanguagePlugin[];

    /**
     * Contribution plugins provided by this plugin
     */
    contributionPlugins: BackendContributionPlugin[];
}

/**
 * Represents a language plugin configuration from the backend.
 */
export interface BackendLanguagePlugin {
    id: string;
    name: string;
    extension: string;
    defaultContent?: string;
    serverPlugin: {
        import: string;
    };
    editorPlugin?: {
        import: string;
        stylesUrl: string;
    };
    languageConfiguration: object;
    monarchTokensProvider: object;
    icon: IconNode;
}

/**
 * Represents a contribution plugin that extends a language with additional functionality.
 */
export interface BackendContributionPlugin {
    id: string;
    languageId: string;
    description: string;
    additionalKeywords: string[];
    serverContributionPlugins: object[];
}

/**
 * Interface returned when resolving a plugin.
 * This will be specified further in the future.
 */
export interface ResolvedPlugin {
    // To be specified in the future
}
