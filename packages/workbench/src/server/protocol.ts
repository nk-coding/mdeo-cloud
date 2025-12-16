import type { ServerPlugin } from "@/data/plugin/serverPlugin";

/**
 * Namespace for the get plugins request
 */
export namespace GetPluginsRequest {
    /**
     * Request method name
     */
    export const method = "extensible/getPlugins";

    /**
     * Request parameters for getting plugin configuration
     */
    export interface Params {
        // No parameters needed for now
    }

    /**
     * Response containing the list of plugins to load
     */
    export interface Response {
        /**
         * The list of server plugins to initialize
         */
        plugins: ServerPlugin[];
    }
}

/**
 * Namespace for the server ready notification
 */
export namespace ServerReadyNotification {
    /**
     * Notification method name
     */
    export const method = "extensible/serverReady";

    /**
     * Notification parameters
     */
    export interface Params {
        // No parameters needed for now
    }
}
