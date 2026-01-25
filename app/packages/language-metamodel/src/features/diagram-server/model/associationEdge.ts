import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType, AssociationEndKind } from "./elementTypes.js";

/**
 * Edge representing an association relationship.
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
     * @param operator The operator type (e.g., -->, <--*, etc.)
     * @returns This builder for chaining
     */
    operator(operator: string): this {
        this.proxy.operator = operator;
        return this;
    }

    /**
     * Sets the kind of decoration at the source end.
     *
     * @param kind The source end kind
     * @returns This builder for chaining
     */
    sourceKind(kind: AssociationEndKind): this {
        this.proxy.sourceKind = kind;
        return this;
    }

    /**
     * Sets the kind of decoration at the target end.
     *
     * @param kind The target end kind
     * @returns This builder for chaining
     */
    targetKind(kind: AssociationEndKind): this {
        this.proxy.targetKind = kind;
        return this;
    }
}
