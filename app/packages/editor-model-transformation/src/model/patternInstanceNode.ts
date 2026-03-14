import { FEEDBACK_EDGE_ID, GRectangularNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";
import type { GEdge } from "@mdeo/editor-shared";
import type { GModelElement } from "@eclipse-glsp/sprotty";
import { PatternModifierKind } from "@mdeo/protocol-model-transformation";
import { GPatternLinkEdge } from "./patternLinkEdge.js";

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
     * The modifier kind (none, create, delete, forbid, require)
     */
    modifier!: PatternModifierKind;

    /**
     * The class hierarchy of this instance (class name and all superclass names).
     * Populated by the server to enable canConnect validation.
     */
    classHierarchy?: string[];

    override canConnect(edge: GEdge, role: "source" | "target"): boolean {
        if (edge instanceof GPatternLinkEdge) {
            if (this.classHierarchy != null) {
                if (role === "source" && edge.sourceClass != null) {
                    if (!this.classHierarchy.includes(edge.sourceClass)) {
                        return false;
                    }
                }
                if (role === "target" && edge.targetClass != null) {
                    if (!this.classHierarchy.includes(edge.targetClass)) {
                        return false;
                    }
                }
            }
            if (edge.id !== FEEDBACK_EDGE_ID && !this.isModifierCompatibleWithEdge(edge.modifier)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether this node's modifier is compatible with the given edge modifier.
     *
     * A node with no modifier ({@link PatternModifierKind.NONE}) can be connected by any edge.
     * A node with a specific modifier may only be connected by edges that share the same modifier.
     *
     * @param edgeModifier The modifier of the edge being connected
     * @returns `true` if forming a connection is allowed, `false` otherwise
     */
    private isModifierCompatibleWithEdge(edgeModifier: PatternModifierKind): boolean {
        return this.modifier === PatternModifierKind.NONE || this.modifier === edgeModifier;
    }
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
