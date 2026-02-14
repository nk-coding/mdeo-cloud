import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Edge representing control flow between nodes.
 * Connects start, match, diamond, merge, and end nodes.
 */
export class GControlFlowEdge extends GEdge {
    /**
     * Creates a builder for GControlFlowEdge instances.
     *
     * @returns A new GControlFlowEdgeBuilder
     */
    static builder(): GControlFlowEdgeBuilder {
        return new GControlFlowEdgeBuilder(GControlFlowEdge).type(ModelTransformationElementType.EDGE_CONTROL_FLOW);
    }
}

/**
 * Builder for GControlFlowEdge instances.
 * Provides fluent API for constructing control flow edges.
 */
export class GControlFlowEdgeBuilder<E extends GControlFlowEdge = GControlFlowEdge> extends GEdgeBuilder<E> {}
