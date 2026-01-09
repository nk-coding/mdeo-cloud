import { GModelElement, GModelElementBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Divider for separating compartments.
 */
export class ClassDivider extends GModelElement {
    /**
     * Creates a builder for ClassDivider instances.
     *
     * @returns A new ClassDividerBuilder
     */
    static builder(): ClassDividerBuilder {
        return new ClassDividerBuilder(ClassDivider).type(MetamodelElementType.DIVIDER);
    }
}

/**
 * Builder for ClassDivider instances.
 * Provides fluent API for constructing class dividers.
 */
export class ClassDividerBuilder<T extends ClassDivider = ClassDivider> extends GModelElementBuilder<T> {}
