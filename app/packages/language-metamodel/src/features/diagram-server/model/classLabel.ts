import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Label for the Class name.
 */
export class GClassLabel extends GLabel {
    /**
     * Creates a builder for GClassLabel instances.
     *
     * @returns A new GClassLabelBuilder
     */
    static builder(): GClassLabelBuilder {
        return new GClassLabelBuilder(GClassLabel).type(MetamodelElementType.LABEL_CLASS_NAME);
    }
}

/**
 * Builder for GClassLabel instances.
 * Provides fluent API for constructing class name labels.
 */
export class GClassLabelBuilder<T extends GClassLabel = GClassLabel> extends GLabelBuilder<T> {}
