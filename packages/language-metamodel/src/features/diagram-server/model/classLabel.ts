import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Label for the Class name.
 */
export class ClassLabel extends GLabel {
    /**
     * Creates a builder for ClassLabel instances.
     * 
     * @returns A new ClassLabelBuilder
     */
    static builder(): ClassLabelBuilder {
        return new ClassLabelBuilder(ClassLabel).type(MetamodelElementType.LABEL_CLASS_NAME);
    }
}

/**
 * Builder for ClassLabel instances.
 * Provides fluent API for constructing class name labels.
 */
export class ClassLabelBuilder<T extends ClassLabel = ClassLabel> extends GLabelBuilder<T> {}
