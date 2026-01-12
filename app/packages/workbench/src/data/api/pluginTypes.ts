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
}

/**
 * Interface returned when resolving a plugin.
 * This will be specified further in the future.
 */
export interface ResolvedPlugin {
    // To be specified in the future
}
