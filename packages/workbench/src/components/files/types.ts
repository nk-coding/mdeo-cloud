/**
 * A type of file which can be handled by the workbench
 */
export interface WorkbenchFileType {
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
