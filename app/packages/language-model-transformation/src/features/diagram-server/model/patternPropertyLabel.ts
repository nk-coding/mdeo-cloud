import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Label for property assignment display in patterns.
 * Shows property name and its assigned/constrained value.
 */
export class GPatternPropertyLabel extends GLabel {
    /**
     * Creates a builder for GPatternPropertyLabel instances.
     *
     * @returns A new GPatternPropertyLabelBuilder
     */
    static builder(): GPatternPropertyLabelBuilder {
        return new GPatternPropertyLabelBuilder(GPatternPropertyLabel).type(
            ModelTransformationElementType.LABEL_PATTERN_PROPERTY
        );
    }
}

/**
 * Builder for GPatternPropertyLabel instances.
 * Provides fluent API for constructing pattern property labels.
 */
export class GPatternPropertyLabelBuilder<
    T extends GPatternPropertyLabel = GPatternPropertyLabel
> extends GLabelBuilder<T> {}
