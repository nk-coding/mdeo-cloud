import { GNode } from "@mdeo/editor-shared";

/**
 * Client-side model for association multiplicity nodes.
 * Represents a multiplicity constraint at the start or end of an association edge.
 */
export class GAssociationMultiplicity extends GNode {
    /**
     * Whether this multiplicity is at the start or end of the association
     */
    target!: "start" | "end";
}
