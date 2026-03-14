import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import type { EndNodeKind } from "@mdeo/protocol-model-transformation";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Node representing the end of a control flow path (stop or kill).
 */
export class GEndNode extends GNode {
    /**
     * The kind of end node (stop or kill)
     */
    kind!: EndNodeKind;

    /**
     * Creates a builder for GEndNode instances.
     *
     * @returns A new GEndNodeBuilder
     */
    static builder(): GEndNodeBuilder {
        return new GEndNodeBuilder(GEndNode).type(ModelTransformationElementType.NODE_END);
    }
}

/**
 * Builder for GEndNode instances.
 * Provides fluent API for constructing end nodes.
 */
export class GEndNodeBuilder<T extends GEndNode = GEndNode> extends GNodeBuilder<T> {
    /**
     * Sets the kind of end node.
     *
     * @param kind The end node kind (stop or kill)
     * @returns This builder for chaining
     */
    kind(kind: EndNodeKind): this {
        this.proxy.kind = kind;
        return this;
    }
}
