import type { WorkspaceEdit } from "vscode-languageserver-types";

/**
 * Container for old and new source/target IDs during reconnection.
 */
export interface ReconnectEndpoints {
    /**
     * The old source element ID before reconnection.
     */
    oldSourceId: string;
    /**
     * The new source element ID after reconnection.
     */
    newSourceId: string;
    /**
     * The old target element ID before reconnection.
     */
    oldTargetId: string;
    /**
     * The new target element ID after reconnection.
     */
    newTargetId: string;
}

/**
 * Result of a reconnect edge operation.
 */
export interface ReconnectEdgeResult {
    /**
     * The new ID of the edge after reconnection.
     * May be the same as the old ID if the edge identity doesn't change.
     */
    newEdgeId: string;
    /**
     * The workspace edit to apply to update the source model.
     */
    workspaceEdit: WorkspaceEdit;
}
