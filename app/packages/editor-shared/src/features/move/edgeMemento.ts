import type { GEdge } from "../../model/edge.js";
import type { RouteComputationResult } from "../edge-routing/edgeRouter.js";

/**
 * Stores before and after snapshots of an edge's routing state.
 * Used to track changes for animation and undo/redo functionality.
 */
export interface EdgeMemento {
    /**
     * The edge element that was modified.
     */
    edge: GEdge;
    /**
     * The state of the edge before the modification.
     */
    before: RouteComputationResult;
    /**
     * The state of the edge after the modification.
     */
    after: RouteComputationResult;
}
