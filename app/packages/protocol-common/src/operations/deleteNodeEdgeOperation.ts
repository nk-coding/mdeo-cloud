import type { Operation } from "@eclipse-glsp/protocol";

/**
 * Operation to delete a node or edge from the diagram.
 *
 * Sent from client when user selects the delete context action.
 * This is a universal action available on all deletable elements.
 *
 * Element types determine which elements can be deleted via type hints:
 * ```
 * { elementTypeId: "node:class", canDelete: true }
 * ```
 *
 * The handler must:
 * 1. Validate element can be deleted
 * 2. Handle cascading deletions (e.g., edges attached to deleted node)
 * 3. Update model state
 * 4. Possibly trigger layout recalculation
 */
export interface DeleteNodeEdgeOperation extends Operation {
    /**
     * The operation kind identifier.
     */
    kind: "deleteNodeEdge";

    /**
     * The ID of the element to delete.
     *
     * Can be a node or edge ID.
     */
    elementId: string;

    /**
     * Optional flag to also delete connected elements.
     *
     * If true:
     * - Deleting a node also deletes all attached edges
     * - May trigger cascade deletion of dependent elements
     *
     * If false:
     * - Attempting to delete with connections may fail
     * - Or connections are orphaned (implementation-specific)
     *
     * Default: true
     */
    deleteConnected?: boolean;

    /**
     * Optional confirmation context.
     *
     * Used to confirm destructive operations without additional prompts.
     * Should be set after user confirms deletion dialog.
     */
    confirmed?: boolean;
}

export namespace DeleteNodeEdgeOperation {
    export const KIND = "deleteNodeEdge";

    /**
     * Payload for creating a delete-node-edge operation.
     */
    export interface Options {
        elementId: string;
        deleteConnected?: boolean;
        confirmed?: boolean;
    }

    /**
     * Creates a delete-node-edge operation.
     *
     * @param options Operation payload
     * @returns Operation instance
     */
    export function create(options: Options): DeleteNodeEdgeOperation {
        return {
            kind: KIND,
            isOperation: true,
            elementId: options.elementId,
            deleteConnected: options.deleteConnected ?? true,
            confirmed: options.confirmed ?? false
        };
    }
}
