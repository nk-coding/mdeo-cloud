import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Node representing a match block in the transformation.
 * Contains pattern elements (instances, links, where clauses, variables) as children.
 */
export class GMatchNode extends GNode {
    /**
     * Whether this is a "for match" (multiple matches iteration)
     */
    multiple!: boolean;

    /**
     * Creates a builder for GMatchNode instances.
     *
     * @returns A new GMatchNodeBuilder
     */
    static builder(): GMatchNodeBuilder {
        return new GMatchNodeBuilder(GMatchNode).type(ModelTransformationElementType.NODE_MATCH);
    }
}

/**
 * Builder for GMatchNode instances.
 * Provides fluent API for constructing match nodes.
 */
export class GMatchNodeBuilder<T extends GMatchNode = GMatchNode> extends GNodeBuilder<T> {
    /**
     * Sets whether this is a multiple match (for match).
     *
     * @param multiple True if this iterates over all matches
     * @returns This builder for chaining
     */
    multiple(multiple: boolean): this {
        this.proxy.multiple = multiple;
        return this;
    }
}
