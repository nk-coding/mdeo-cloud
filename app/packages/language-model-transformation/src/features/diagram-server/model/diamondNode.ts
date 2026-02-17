import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Diamond node for control flow branching (if/while expression).
 * Represents a decision point in the control flow graph.
 */
export class GDiamondNode extends GNode {
    /**
     * The expression text displayed in the diamond
     */
    expression!: string;

    /**
     * Creates a builder for GDiamondNode instances.
     *
     * @returns A new GDiamondNodeBuilder
     */
    static builder(): GDiamondNodeBuilder {
        return new GDiamondNodeBuilder(GDiamondNode).type(ModelTransformationElementType.NODE_DIAMOND);
    }
}

/**
 * Builder for GDiamondNode instances.
 * Provides fluent API for constructing diamond nodes.
 */
export class GDiamondNodeBuilder<T extends GDiamondNode = GDiamondNode> extends GNodeBuilder<T> {
    /**
     * Sets the expression text for the diamond.
     *
     * @param expression The condition expression text
     * @returns This builder for chaining
     */
    expression(expression: string): this {
        this.proxy.expression = expression;
        return this;
    }
}
