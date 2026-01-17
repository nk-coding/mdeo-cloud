import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Label for property names and types.
 */
export class GPropertyLabel extends GLabel {
    /**
     * Creates a builder for GPropertyLabel instances.
     *
     * @returns A new GPropertyLabelBuilder
     */
    static builder(): GPropertyLabelBuilder {
        return new GPropertyLabelBuilder(GPropertyLabel).type(MetamodelElementType.LABEL_PROPERTY);
    }
}

/**
 * Builder for GPropertyLabel instances.
 * Provides fluent API for constructing property labels.
 */
export class GPropertyLabelBuilder<T extends GPropertyLabel = GPropertyLabel> extends GLabelBuilder<T> {}
