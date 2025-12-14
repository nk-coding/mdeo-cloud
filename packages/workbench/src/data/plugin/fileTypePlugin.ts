/**
 * A plugin for a file type handled by the workbench
 */
export interface FileTypePlugin {
    /**
     * Unique identifier for the file type
     */
    id: string;
    /**
     * The name of the file type, displayed in the UI
     */
    name: string;
    /**
     * The file extension associated with the file type (including the dot)
     */
    extension: string;
    /**
     * Optional default content for new files of this type
     */
    defaultContent?: string;
}
