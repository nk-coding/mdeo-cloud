import type { GModelElement } from "@eclipse-glsp/sprotty";
import { GRectangularNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";
import type { GEdge } from "@mdeo/editor-shared";
import { GLinkEdge } from "./linkEdge.js";

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

    /**
     * The class hierarchy of this instance (class name and all superclass names).
     * Populated by the server to enable canConnect validation.
     */
    classHierarchy?: string[];

    override canConnect(edge: GEdge, role: "source" | "target"): boolean {
        if (edge instanceof GLinkEdge && this.classHierarchy != null) {
            if (role === "source" && edge.sourceClass != null) {
                return this.classHierarchy.includes(edge.sourceClass);
            }
            if (role === "target" && edge.targetClass != null) {
                return this.classHierarchy.includes(edge.targetClass);
            }
        }
        return true;
    }
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
