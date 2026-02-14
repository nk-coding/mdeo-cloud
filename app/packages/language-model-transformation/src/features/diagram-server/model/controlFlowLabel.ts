import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Label for control flow edges.
 * Displays labels like "true", "false", "else", etc. on control flow edges.
 */
export class GControlFlowLabel extends GLabel {
    /**
     * Creates a builder for GControlFlowLabel instances.
     *
     * @returns A new GControlFlowLabelBuilder
     */
    static builder(): GControlFlowLabelBuilder {
        return new GControlFlowLabelBuilder(GControlFlowLabel).type(ModelTransformationElementType.LABEL_CONTROL_FLOW);
    }
}

/**
 * Builder for GControlFlowLabel instances.
 * Provides fluent API for constructing control flow labels.
 */
export class GControlFlowLabelBuilder<T extends GControlFlowLabel = GControlFlowLabel> extends GLabelBuilder<T> {}
