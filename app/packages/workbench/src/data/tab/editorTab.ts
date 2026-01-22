import type { Uri } from "vscode";

/**
 * A tab in the editor
 */
export interface EditorTab {
    /**
     * The URI of the file being edited in this tab
     */
    fileUri: Uri;
    /**
     * Whether this is a temporarily opened tab
     */
    temporary: boolean;
}
