import type { GModelElement } from "@eclipse-glsp/sprotty";
import { GRectangularNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { connectableFeature, deletableFeature, selectFeature, boundsFeature, moveFeature, fadeFeature } =
    sharedImport("@eclipse-glsp/sprotty");
const { resizeFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Client-side model for a Class node.
 * Represents a class in the metamodel diagram with properties displayed as labels.
 */
export class GClassNode extends GRectangularNode {
    static override readonly DEFAULT_FEATURES = [
        connectableFeature,
        deletableFeature,
        selectFeature,
        boundsFeature,
        moveFeature,
        fadeFeature,
        resizeFeature,
        nodeLayoutMetadataFeature
    ];

    name?: string;
    isAbstract?: boolean;
}

export function isClassNode(element: GModelElement): element is GClassNode {
    return element instanceof GClassNode;
}
