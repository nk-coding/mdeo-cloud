import type { File } from "../filesystem/file";

/**
 * A tab in the editor
 */
export interface EditorTab {
    /**
     * The file being edited in this tab
     */
    file: File;
    /**
     * Whether this is a temporarily opened tab
     */
    temporary: boolean;
}
