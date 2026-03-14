import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "@mdeo/protocol-metamodel";

/**
 * Node for association property information.
 * Represents a property name at the start or end of an association edge.
 */
export class GAssociationPropertyNode extends GNode {
    /**
     * Whether this property is at the start or end of the association
     */
    end!: "source" | "target";

    /**
     * Creates a builder for GAssociationPropertyNode instances.
     *
     * @returns A new GAssociationPropertyNodeBuilder
     */
    static builder(): GAssociationPropertyNodeBuilder {
        return new GAssociationPropertyNodeBuilder(GAssociationPropertyNode).type(
            MetamodelElementType.NODE_ASSOCIATION_PROPERTY
        );
    }
}

/**
 * Builder for GAssociationPropertyNode instances.
 * Provides fluent API for constructing association property nodes.
 */
export class GAssociationPropertyNodeBuilder<
    T extends GAssociationPropertyNode = GAssociationPropertyNode
> extends GNodeBuilder<T> {
    /**
     * Sets the target for the property node.
     *
     * @param end Whether this is at start or end
     * @returns This builder for chaining
     */
    end(end: "source" | "target"): this {
        this.proxy.end = end;
        return this;
    }
}
