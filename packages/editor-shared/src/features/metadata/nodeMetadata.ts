import type { GModelElement, Point } from "@eclipse-glsp/sprotty";

/**
 * Base metadata interface for node position.
 */
export interface NodePositionMetadata {
    /**
     * The position of the node in the diagram.
     */
    position: Point;
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
 * Symbol to identify the node layout metadata feature.
 */
export const nodeLayoutMetadataFeature = Symbol("nodeLayoutMetadataFeature");

/**
 * Checks if the given element has layout metadata.
 *
 * @param element The model element to check
 * @returns True if the element has layout metadata, false otherwise
 */
export function hasNodeLayoutMetadata(element: GModelElement): element is GModelElement & { meta: NodeLayoutMetadata } {
    return element.hasFeature(nodeLayoutMetadataFeature);
}
