import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Node representing the start of the transformation.
 */
export class GStartNode extends GNode {
    /**
     * Creates a builder for GStartNode instances.
     *
     * @returns A new GStartNodeBuilder
     */
    static builder(): GStartNodeBuilder {
        return new GStartNodeBuilder(GStartNode).type(ModelTransformationElementType.NODE_START);
    }
}

/**
 * Builder for GStartNode instances.
 * Provides fluent API for constructing start nodes.
 */
export class GStartNodeBuilder<T extends GStartNode = GStartNode> extends GNodeBuilder<T> {}
