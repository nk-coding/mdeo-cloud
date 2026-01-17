import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Edge representing an association relationship.
 */
export class GAssociationEdge extends GEdge {
    /**
     * The association operator (e.g., composition, aggregation)
     */
    operator?: string;

    /**
     * Creates a builder for GAssociationEdge instances.
     *
     * @returns A new GAssociationEdgeBuilder
     */
    static builder(): GAssociationEdgeBuilder {
        return new GAssociationEdgeBuilder(GAssociationEdge).type(MetamodelElementType.EDGE_ASSOCIATION);
    }
}

/**
 * Builder for GAssociationEdge instances.
 * Provides fluent API for constructing association edges.
 */
export class GAssociationEdgeBuilder<E extends GAssociationEdge = GAssociationEdge> extends GEdgeBuilder<E> {
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
