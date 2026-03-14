import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "@mdeo/protocol-model";

/**
 * Node for link end label (source or target).
 * Wraps the end label to provide proper bounds handling.
 */
export class GLinkEndNode extends GNode {
    /**
     * Whether this is at the source or target end of the link
     */
    end!: "source" | "target";

    /**
     * Creates a builder for GLinkEndNode instances.
     *
     * @returns A new GLinkEndNodeBuilder
     */
    static builder(): GLinkEndNodeBuilder {
        return new GLinkEndNodeBuilder(GLinkEndNode).type(ModelElementType.NODE_LINK_END);
    }
}

/**
 * Builder for GLinkEndNode instances.
 * Provides fluent API for constructing link end nodes.
 */
export class GLinkEndNodeBuilder<T extends GLinkEndNode = GLinkEndNode> extends GNodeBuilder<T> {
    /**
     * Sets the end for the node.
     *
     * @param end Whether this is at source or target
     * @returns This builder for chaining
     */
    end(end: "source" | "target"): this {
        this.proxy.end = end;
        return this;
    }
}
