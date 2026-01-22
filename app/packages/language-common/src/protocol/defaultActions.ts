/**
 * Data for the new file action trigger.
 * Passed when a new file is created and newFileAction is enabled.
 */
export interface NewFileActionData {
    /**
     * The URI of the newly created file
     */
    uri: string;
}

/**
 * Data for file menu actions.
 * Passed when an action is triggered from the file context menu or the tabs bar
 */
export interface FileMenuActionData {
    /**
     * The URI of the file on which the action is triggered
     */
    uri: string;
}
