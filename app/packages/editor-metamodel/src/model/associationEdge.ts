import { GEdge } from "@mdeo/editor-shared";
import { AssociationEndKind } from "@mdeo/protocol-metamodel";

/**
 * Client-side model for association edges.
 */
export class GAssociationEdge extends GEdge {
    /**
     * The association operator (e.g., -->, <--*, etc.)
     */
    operator?: string;

    /**
     * The kind of decoration at the source end of the edge.
     */
    sourceKind: AssociationEndKind = AssociationEndKind.NONE;

    /**
     * The kind of decoration at the target end of the edge.
     */
    targetKind: AssociationEndKind = AssociationEndKind.NONE;
}
