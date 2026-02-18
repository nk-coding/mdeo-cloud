import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";
import type { GModelElement } from "@eclipse-glsp/sprotty";

const {
    connectableFeature,
    deletableFeature,
    selectFeature,
    boundsFeature,
    moveFeature,
    fadeFeature,
    layoutContainerFeature
} = sharedImport("@eclipse-glsp/sprotty");
const { resizeFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Client-side model for a match node in the transformation diagram.
 * Match nodes contain pattern elements (instances, links) as children,
 * and may also contain constraint compartments (variables, where clauses).
 */
export class GMatchNode extends GNode {
    /**
     * Default features enabled for match nodes
     */
    static readonly DEFAULT_FEATURES = [
        connectableFeature,
        deletableFeature,
        selectFeature,
        boundsFeature,
        moveFeature,
        fadeFeature,
        resizeFeature,
        nodeLayoutMetadataFeature,
        layoutContainerFeature
    ];

    /**
     * The name/label for this match block (e.g., "match", "for match", "if match")
     */
    label!: string;

    /**
     * Whether this is a "for match" (multiple matches iteration)
     */
    multiple!: boolean;
}

/**
 * Type guard to check if an element is a match node.
 *
 * @param element The model element to check
 * @returns True if the element is a GMatchNode
 */
export function isMatchNode(element: GModelElement): element is GMatchNode {
    return element instanceof GMatchNode;
}
