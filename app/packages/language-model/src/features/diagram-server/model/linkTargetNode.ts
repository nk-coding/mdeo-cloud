import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "./elementTypes.js";

/**
 * Node for link target label.
 * Wraps the target label to provide proper bounds handling.
 */
export class GLinkTargetNode extends GNode {
    /**
     * Creates a builder for GLinkTargetNode instances.
     *
     * @returns A new GLinkTargetNodeBuilder
     */
    static builder(): GLinkTargetNodeBuilder {
        return new GLinkTargetNodeBuilder(GLinkTargetNode).type(ModelElementType.NODE_LINK_TARGET);
    }
}

/**
 * Builder for GLinkTargetNode instances.
 * Provides fluent API for constructing link target nodes.
 */
export class GLinkTargetNodeBuilder<T extends GLinkTargetNode = GLinkTargetNode> extends GNodeBuilder<T> {}
