import type { GModelElement } from "@eclipse-glsp/sprotty";
import { GRectangularNode, sharedImport } from "@mdeo/editor-shared";

const {
    connectableFeature,
    deletableFeature,
    selectFeature,
    boundsFeature,
    moveFeature,
    layoutContainerFeature,
    fadeFeature
} = sharedImport("@eclipse-glsp/sprotty");

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
        layoutContainerFeature,
        fadeFeature
    ];

    name?: string;
    isAbstract?: boolean;
}

export function isClassNode(element: GModelElement): element is GClassNode {
    return element instanceof GClassNode;
}
