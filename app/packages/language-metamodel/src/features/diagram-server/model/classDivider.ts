import { GModelElement, GModelElementBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Divider for separating compartments.
 */
export class GClassDivider extends GModelElement {
    /**
     * Creates a builder for GClassDivider instances.
     *
     * @returns A new GClassDividerBuilder
     */
    static builder(): GClassDividerBuilder {
        return new GClassDividerBuilder(GClassDivider).type(MetamodelElementType.DIVIDER);
    }
}

/**
 * Builder for GClassDivider instances.
 * Provides fluent API for constructing class dividers.
 */
export class GClassDividerBuilder<T extends GClassDivider = GClassDivider> extends GModelElementBuilder<T> {}
