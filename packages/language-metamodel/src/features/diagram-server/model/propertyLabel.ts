import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Label for property names and types.
 */
export class PropertyLabel extends GLabel {
    /**
     * Creates a builder for PropertyLabel instances.
     * 
     * @returns A new PropertyLabelBuilder
     */
    static builder(): PropertyLabelBuilder {
        return new PropertyLabelBuilder(PropertyLabel).type(MetamodelElementType.LABEL_PROPERTY);
    }
}

/**
 * Builder for PropertyLabel instances.
 * Provides fluent API for constructing property labels.
 */
export class PropertyLabelBuilder<T extends PropertyLabel = PropertyLabel> extends GLabelBuilder<T> {}
