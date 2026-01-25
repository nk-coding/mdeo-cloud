import type { GModelElement } from "@eclipse-glsp/sprotty";
import { GRectangularNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { deletableFeature, selectFeature, boundsFeature, moveFeature, fadeFeature } =
    sharedImport("@eclipse-glsp/sprotty");
const { resizeFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Client-side model for an Enum node.
 * Represents an enum in the metamodel diagram with entries displayed as labels.
 */
export class GEnumNode extends GRectangularNode {
    static readonly DEFAULT_FEATURES = [
        deletableFeature,
        selectFeature,
        boundsFeature,
        moveFeature,
        fadeFeature,
        resizeFeature,
        nodeLayoutMetadataFeature
    ];

    name?: string;
}

/**
 * Type guard for checking if an element is a GEnumNode.
 *
 * @param element The element to check
 * @returns True if the element is a GEnumNode
 */
export function isEnumNode(element: GModelElement): element is GEnumNode {
    return element instanceof GEnumNode;
}
