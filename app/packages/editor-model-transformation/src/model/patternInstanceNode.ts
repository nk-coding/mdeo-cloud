import { GRectangularNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";
import type { GModelElement } from "@eclipse-glsp/sprotty";
import { PatternModifierKind } from "./elementTypes.js";

const { connectableFeature, deletableFeature, selectFeature, boundsFeature, moveFeature, fadeFeature } =
    sharedImport("@eclipse-glsp/sprotty");
const { resizeFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Client-side model for a pattern instance node.
 * Represents an object instance in a pattern (match/create/delete) with a name, optional type, and modifier.
 */
export class GPatternInstanceNode extends GRectangularNode {
    /**
     * Default features enabled for pattern instance nodes
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
     * The name of the pattern instance
     */
    name!: string;

    /**
     * The optional type name (class name) of the pattern instance
     */
    typeName?: string;

    /**
     * The modifier kind (none, create, delete, forbid)
     */
    modifier!: PatternModifierKind;
}

/**
 * Type guard to check if an element is a pattern instance node.
 *
 * @param element The model element to check
 * @returns True if the element is a GPatternInstanceNode
 */
export function isPatternInstanceNode(element: GModelElement): element is GPatternInstanceNode {
    return element instanceof GPatternInstanceNode;
}
