import { GEdge } from "@mdeo/editor-shared";

/**
 * Client-side model for link edges between objects.
 * Represents a reference from one object to another in the model diagram.
 */
export class GLinkEdge extends GEdge {
    /**
     * The name of the link (reference property name)
     */
    linkName?: string;

    /**
     * The name of the metamodel class required at the source of this link type.
     * Used by canConnect to validate source node eligibility.
     */
    sourceClass?: string;

    /**
     * The name of the metamodel class required at the target of this link type.
     * Used by canConnect to validate target node eligibility.
     */
    targetClass?: string;
}
