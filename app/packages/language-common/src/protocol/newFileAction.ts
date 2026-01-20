/**
 * Data for the new file action trigger.
 * Passed when a new file is created and newFileAction is enabled.
 */
export interface NewFileActionData {
    /**
     * The absolute URI path of the newly created file
     */
    filePath: string;
}
