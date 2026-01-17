import type { GModelElement } from "@eclipse-glsp/sprotty";
import type { GEdge } from "../../model/edge.js";

/**
 * Checks if an element is an edge with the expected meta property.
 */
export function isEdgeWithMeta(element: GModelElement | undefined): element is GEdge {
    return element !== undefined && "meta" in element && "sourceId" in element && "targetId" in element;
}
