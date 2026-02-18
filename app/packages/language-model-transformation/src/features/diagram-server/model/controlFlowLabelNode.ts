import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Node wrapping a control flow edge label.
 * Provides proper bounds handling for labels on control flow edges.
 */
export class GControlFlowLabelNode extends GNode {
    /**
     * Whether this label is at the source or target end of the edge
     */
    end!: "source" | "target";

    /**
     * Creates a builder for GControlFlowLabelNode instances.
     *
     * @returns A new GControlFlowLabelNodeBuilder
     */
    static builder(): GControlFlowLabelNodeBuilder {
        return new GControlFlowLabelNodeBuilder(GControlFlowLabelNode).type(
            ModelTransformationElementType.NODE_CONTROL_FLOW_LABEL
        );
    }
}

/**
 * Builder for GControlFlowLabelNode instances.
 * Provides fluent API for constructing control flow label nodes.
 */
export class GControlFlowLabelNodeBuilder<
    T extends GControlFlowLabelNode = GControlFlowLabelNode
> extends GNodeBuilder<T> {
    /**
     * Sets the end position for the label node.
     *
     * @param end Whether this is at source or target
     * @returns This builder for chaining
     */
    end(end: "source" | "target"): this {
        this.proxy.end = end;
        return this;
    }
}
