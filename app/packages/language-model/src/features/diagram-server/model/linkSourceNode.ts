import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "./elementTypes.js";

/**
 * Node for link source label.
 * Wraps the source label to provide proper bounds handling.
 */
export class GLinkSourceNode extends GNode {
    /**
     * Creates a builder for GLinkSourceNode instances.
     *
     * @returns A new GLinkSourceNodeBuilder
     */
    static builder(): GLinkSourceNodeBuilder {
        return new GLinkSourceNodeBuilder(GLinkSourceNode).type(ModelElementType.NODE_LINK_SOURCE);
    }
}

/**
 * Builder for GLinkSourceNode instances.
 * Provides fluent API for constructing link source nodes.
 */
export class GLinkSourceNodeBuilder<T extends GLinkSourceNode = GLinkSourceNode> extends GNodeBuilder<T> {}
