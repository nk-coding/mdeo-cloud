import type { FileTypePlugin } from "./fileTypePlugin";

/**
 * Plugin interface for the workbench
 */
export interface Plugin {
    /**
     * The id of the plugin
     */
    id: string;
    /**
     * Contributed file types by this plugin
     */
    fileTypes: FileTypePlugin[];
}
