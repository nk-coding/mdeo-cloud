import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Split node for control flow branching (if/while expression).
 * Represents a decision point in the control flow graph.
 */
export class GSplitNode extends GNode {
    /**
     * The expression text displayed in the diamond
     */
    expression!: string;

    /**
     * Creates a builder for GSplitNode instances.
     *
     * @returns A new GSplitNodeBuilder
     */
    static builder(): GSplitNodeBuilder {
        return new GSplitNodeBuilder(GSplitNode).type(ModelTransformationElementType.NODE_SPLIT);
    }
}

/**
 * Builder for GSplitNode instances.
 * Provides fluent API for constructing split nodes.
 */
export class GSplitNodeBuilder<T extends GSplitNode = GSplitNode> extends GNodeBuilder<T> {
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
