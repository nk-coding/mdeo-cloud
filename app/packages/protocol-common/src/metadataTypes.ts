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
 * Side of a rectangular node for edge anchoring.
 */
export type AnchorSide = "top" | "left" | "bottom" | "right";

/**
 * Anchor point configuration for edges.
 * Specifies where an edge connects to a node.
 */
export interface EdgeAnchor {
    /**
     * The side of the node where the edge connects.
     */
    side: AnchorSide;
    /**
     * Position along the side, ranging from 0 to 1.
     * 0 represents the start (left/top), 1 represents the end (right/bottom),
     * and 0.5 represents the center of the side.
     */
    value: number;
}

/**
 * Metadata interface for edge visual properties.
 * Contains routing information for graph edges.
 */
export interface EdgeLayoutMetadata {
    /**
     * The routing points for the edge.
     * These define the path the edge takes from source to target.
     */
    routingPoints: Point[];
    /**
     * Optional anchor point at the source of the edge.
     */
    sourceAnchor?: EdgeAnchor;
    /**
     * Optional anchor point at the target of the edge.
     */
    targetAnchor?: EdgeAnchor;
}
