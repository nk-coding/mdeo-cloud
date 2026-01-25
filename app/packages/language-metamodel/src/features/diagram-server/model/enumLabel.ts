import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Label for the Enum name.
 */
export class GEnumLabel extends GLabel {
    /**
     * Creates a builder for GEnumLabel instances.
     *
     * @returns A new GEnumLabelBuilder
     */
    static builder(): GEnumLabelBuilder {
        return new GEnumLabelBuilder(GEnumLabel).type(MetamodelElementType.LABEL_ENUM_NAME);
    }
}

/**
 * Builder for GEnumLabel instances.
 * Provides fluent API for constructing enum name labels.
 */
export class GEnumLabelBuilder<T extends GEnumLabel = GEnumLabel> extends GLabelBuilder<T> {}
