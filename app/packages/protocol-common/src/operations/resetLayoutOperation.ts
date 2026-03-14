import type { Operation } from "@eclipse-glsp/protocol";

/**
 * Operation to reset the layout of a node or routing of an edge.
 *
 * Sent from client when user selects the reset layout context action.
 * This is a universal action available on all elements with layout.
 *
 * For nodes:
 * - Resets to default size (may trigger auto-layout)
 * - Clears any manual sizing
 *
 * For edges:
 * - Resets routing points to default path
 * - Clears manual routing
 * - Re-computes optimal path based on anchors
 *
 * Type hints determine which elements support this:
 * ```
 * { elementTypeId: "node:class", canResetLayout: true }
 * ```
 */
export interface ResetLayoutOperation extends Operation {
    /**
     * The operation kind identifier.
     */
    kind: "resetLayout";

    /**
     * The ID of the element to reset.
     *
     * Can be a node or edge ID.
     */
    elementId: string;

    /**
     * Scope of the reset.
     *
     * "bounds": Reset size and position (for nodes)
     * "routing": Reset edge path (for edges)
     * "all": Reset all layout properties (default)
     */
    scope?: "bounds" | "routing" | "all";

    /**
     * Optional animation duration in milliseconds.
     *
     * If provided, layout changes animate over this duration.
     * If not provided or 0, changes are instant.
     *
     * Default: 300ms
     */
    animationDuration?: number;
}

export namespace ResetLayoutOperation {
    export const KIND = "resetLayout";

    /**
     * Payload for creating a reset-layout operation.
     */
    export interface Options {
        elementId: string;
        scope?: "bounds" | "routing" | "all";
        animationDuration?: number;
    }

    /**
     * Creates a reset-layout operation.
     *
     * @param options Operation payload
     * @returns Operation instance
     */
    export function create(options: Options): ResetLayoutOperation {
        return {
            kind: KIND,
            isOperation: true,
            elementId: options.elementId,
            scope: options.scope ?? "all",
            animationDuration: options.animationDuration ?? 300
        };
    }
}
