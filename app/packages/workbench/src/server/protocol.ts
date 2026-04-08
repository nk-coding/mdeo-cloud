import type { ServerPlugin } from "@/data/plugin/serverPlugin";
import { NotificationType, RequestType } from "vscode-languageserver-protocol";
import type { ActionStartParams } from "@mdeo/language-common";
import type { Range } from "vscode-languageserver-types";

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
    export type Params = Record<string, never>;

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
    export type Params = Record<string, never>;
}

/**
 * Request parameters for reading a file
 */
export interface ReadFileParams {
    /**
     * The URI of the file to read
     */
    uri: string;
}

/**
 * Namespace for the read file request
 */
export namespace ReadFileRequest {
    /**
     * Request type for reading a file
     */
    export const type = new RequestType<ReadFileParams, string, any>("fs/readFile");
}

/**
 * Request parameters for getting file/directory stats
 */
export interface StatParams {
    /**
     * The URI of the file or directory to stat
     */
    uri: string;
}

/**
 * Response containing the file system node information
 */
export interface StatResponse {
    /**
     * Whether this is a file
     */
    isFile: boolean;
    /**
     * Whether this is a directory
     */
    isDirectory: boolean;
    /**
     * The URI of the file or directory
     */
    uri: string;
}

/**
 * Namespace for the stat request
 */
export namespace StatRequest {
    /**
     * Request type for getting file/directory stats
     */
    export const type = new RequestType<StatParams, StatResponse, any>("fs/stat");
}

/**
 * Request parameters for reading a directory
 */
export interface ReadDirectoryParams {
    /**
     * The URI of the directory to read
     */
    uri: string;
}

/**
 * File system node information
 */
export interface FileSystemNode {
    /**
     * Whether this is a file
     */
    isFile: boolean;
    /**
     * Whether this is a directory
     */
    isDirectory: boolean;
    /**
     * The URI of the file or directory
     */
    uri: string;
}

/**
 * Namespace for the read directory request
 */
export namespace ReadDirectoryRequest {
    /**
     * Request type for reading a directory
     */
    export const type = new RequestType<ReadDirectoryParams, FileSystemNode[], any>("fs/readDirectory");
}

/**
 * Request parameters for reading file metadata
 */
export interface ReadMetadataParams {
    /**
     * The URI of the file to read metadata for
     */
    uri: string;
}

/**
 * Namespace for the read metadata request
 */
export namespace ReadMetadataRequest {
    /**
     * Request type for reading file metadata
     */
    export const type = new RequestType<ReadMetadataParams, object, any>("fs/readMetadata");
}

/**
 * Request parameters for writing file metadata
 */
export interface WriteMetadataParams {
    /**
     * The URI of the file to write metadata for
     */
    uri: string;
    /**
     * The metadata object to write
     */
    metadata: object;
}

/**
 * Namespace for the write metadata request
 */
export namespace WriteMetadataRequest {
    /**
     * Request type for writing file metadata
     */
    export const type = new RequestType<WriteMetadataParams, void, any>("fs/writeMetadata");
}

/**
 * Namespace for the trigger action notification.
 * This notification is sent from the language server to trigger an action dialog in the workbench.
 */
export namespace TriggerActionNotification {
    /**
     * Notification type for triggering an action dialog
     */
    export const type = new NotificationType<ActionStartParams>("action/trigger");
}

/**
 * Parameters for the reveal-source notification.
 */
export interface RevealSourceParams {
    /**
     * URI of the document whose source should be revealed.
     */
    uri: string;
    /**
     * LSP range within the document that should be selected and scrolled into view.
     */
    range: Range;
}

/**
 * Namespace for the reveal-source notification.
 * Sent from the language server to the workbench when a graphical element has been
 * alt-clicked or its issue marker has been double-clicked, instructing the workbench
 * to select and reveal the corresponding source range in the Monaco textual editor.
 */
export namespace RevealSourceNotification {
    /**
     * Notification type for revealing a source range in the Monaco editor.
     */
    export const type = new NotificationType<RevealSourceParams>("textDocument/revealSource");
}
