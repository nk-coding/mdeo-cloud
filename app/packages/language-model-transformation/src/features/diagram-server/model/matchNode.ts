import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Node representing a match block in the transformation.
 * Contains pattern elements (instances, links, where clauses, variables) as children.
 */
export class GMatchNode extends GNode {
    /**
     * The name/label for this match block (e.g., "match", "for match")
     */
    label!: string;

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
     * Sets the label for the match block.
     *
     * @param label The label text
     * @returns This builder for chaining
     */
    label(label: string): this {
        this.proxy.label = label;
        return this;
    }

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
