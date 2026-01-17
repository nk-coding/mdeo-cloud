import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Node for association multiplicity information.
 * Represents a multiplicity constraint at the start or end of an association edge.
 */
export class GAssociationMultiplicityNode extends GNode {
    /**
     * Whether this multiplicity is at the start or end of the association
     */
    target!: "start" | "end";

    /**
     * Creates a builder for GAssociationMultiplicityNode instances.
     *
     * @returns A new GAssociationMultiplicityNodeBuilder
     */
    static builder(): GAssociationMultiplicityNodeBuilder {
        return new GAssociationMultiplicityNodeBuilder(GAssociationMultiplicityNode).type(
            MetamodelElementType.NODE_ASSOCIATION_MULTIPLICITY
        );
    }
}

/**
 * Builder for GAssociationMultiplicityNode instances.
 * Provides fluent API for constructing association multiplicity nodes.
 */
export class GAssociationMultiplicityNodeBuilder<
    T extends GAssociationMultiplicityNode = GAssociationMultiplicityNode
> extends GNodeBuilder<T> {
    /**
     * Sets the target for the multiplicity node.
     *
     * @param target Whether this is at start or end
     * @returns This builder for chaining
     */
    target(target: "start" | "end"): this {
        this.proxy.target = target;
        return this;
    }
}
