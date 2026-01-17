import { GNode } from "@mdeo/editor-shared";

/**
 * Client-side model for association property nodes.
 * Represents a property name at the start or end of an association edge.
 */
export class GAssociationProperty extends GNode {
    /**
     * Whether this property is at the start or end of the association
     */
    target!: "start" | "end";
}
