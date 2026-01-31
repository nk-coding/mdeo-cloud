import type { GModelElement } from "@eclipse-glsp/sprotty";
import { GRectangularNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { connectableFeature, deletableFeature, selectFeature, boundsFeature, moveFeature, fadeFeature } =
    sharedImport("@eclipse-glsp/sprotty");
const { resizeFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Client-side model for an Object node.
 * Represents an object instance in the model diagram with a name and type.
 */
export class GObjectNode extends GRectangularNode {
    /**
     * Default features enabled for object nodes
     */
    static readonly DEFAULT_FEATURES = [
        connectableFeature,
        deletableFeature,
        selectFeature,
        boundsFeature,
        moveFeature,
        fadeFeature,
        resizeFeature,
        nodeLayoutMetadataFeature
    ];

    /**
     * The name of the object instance
     */
    name?: string;

    /**
     * The type name (class) of the object
     */
    typeName?: string;
}

/**
 * Type guard to check if an element is an object node.
 *
 * @param element The model element to check
 * @returns True if the element is a GObjectNode
 */
export function isObjectNode(element: GModelElement): element is GObjectNode {
    return element instanceof GObjectNode;
}
