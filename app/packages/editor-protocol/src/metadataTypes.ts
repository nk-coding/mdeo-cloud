import type { Point } from "@eclipse-glsp/protocol";

/**
 * Base metadata interface for node position.
 */
export interface NodePositionMetadata {
    /**
     * The position of the node in the diagram.
     */
    position?: Point;
}

/**
 * Extended metadata interface for node layout with sizing.
 */
export interface NodeLayoutMetadata extends NodePositionMetadata {
    /**
     * The preferred width of the node.
     * If not specified, the node will be sized automatically.
     */
    prefWidth?: number;
    /**
     * The preferred height of the node.
     * If not specified, the node will be sized autonatically
     */
    prefHeight?: number;
}

/**
 * Metadata interface for edge visual properties.
 * Contains routing information for graph edges.
 */
export interface EdgeVisualMetadata {
    /**
     * The routing points for the edge.
     * These define the path the edge takes from source to target.
     */
    routingPoints: Point[];
}

/**
 * Metadata interface for edge placement properties.
 * Defines positioning along an edge for elements like labels or decorations.
 */
export interface EdgePlacementMetadata {
    /**
     * The position along the edge (0.0 = start, 1.0 = end).
     */
    position: number;

    /**
     * The side of the edge the element appears on.
     */
    side?: "left" | "right" | "top" | "bottom";

    /**
     * The offset from the edge in pixels.
     */
    offset?: number;
}
