import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Label for the pattern instance, displaying name and optional type.
 * Format: "name" or "name : type"
 */
export class GPatternInstanceNameLabel extends GLabel {
    /**
     * Creates a builder for GPatternInstanceNameLabel instances.
     *
     * @returns A new GPatternInstanceNameLabelBuilder
     */
    static builder(): GPatternInstanceNameLabelBuilder {
        return new GPatternInstanceNameLabelBuilder(GPatternInstanceNameLabel).type(
            ModelTransformationElementType.LABEL_PATTERN_INSTANCE_NAME
        );
    }
}

/**
 * Builder for GPatternInstanceNameLabel instances.
 * Provides fluent API for constructing pattern instance name labels.
 */
export class GPatternInstanceNameLabelBuilder<
    T extends GPatternInstanceNameLabel = GPatternInstanceNameLabel
> extends GLabelBuilder<T> {}
