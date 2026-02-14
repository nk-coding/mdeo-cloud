import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Node for pattern link end label (source or target).
 * Wraps the end label to provide proper bounds handling.
 */
export class GPatternLinkEndNode extends GNode {
    /**
     * Whether this is at the source or target end of the link
     */
    end!: "source" | "target";

    /**
     * Creates a builder for GPatternLinkEndNode instances.
     *
     * @returns A new GPatternLinkEndNodeBuilder
     */
    static builder(): GPatternLinkEndNodeBuilder {
        return new GPatternLinkEndNodeBuilder(GPatternLinkEndNode).type(
            ModelTransformationElementType.NODE_PATTERN_LINK_END
        );
    }
}

/**
 * Builder for GPatternLinkEndNode instances.
 * Provides fluent API for constructing pattern link end nodes.
 */
export class GPatternLinkEndNodeBuilder<T extends GPatternLinkEndNode = GPatternLinkEndNode> extends GNodeBuilder<T> {
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
