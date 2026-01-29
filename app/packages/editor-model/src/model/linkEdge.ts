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
}
