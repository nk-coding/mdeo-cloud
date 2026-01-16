import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Edge representing an association relationship.
 */
export class AssociationEdge extends GEdge {
    /**
     * The association operator (e.g., composition, aggregation) 
     */
    operator?: string;

    /**
     * Creates a builder for AssociationEdge instances.
     *
     * @returns A new AssociationEdgeBuilder
     */
    static override builder(): AssociationEdgeBuilder {
        return new AssociationEdgeBuilder(AssociationEdge).type(MetamodelElementType.EDGE_ASSOCIATION);
    }
}

/**
 * Builder for AssociationEdge instances.
 * Provides fluent API for constructing association edges.
 */
export class AssociationEdgeBuilder<E extends AssociationEdge = AssociationEdge> extends GEdgeBuilder<E> {
    /**
     * Sets the association operator.
     *
     * @param operator The operator type (e.g., composition, aggregation)
     * @returns This builder for chaining
     */
    operator(operator: string): this {
        this.proxy.operator = operator;
        return this;
    }
}
