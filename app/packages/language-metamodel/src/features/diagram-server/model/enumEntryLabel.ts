import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "@mdeo/protocol-metamodel";

/**
 * Label for an enum entry.
 */
export class GEnumEntryLabel extends GLabel {
    /**
     * Creates a builder for GEnumEntryLabel instances.
     *
     * @returns A new GEnumEntryLabelBuilder
     */
    static builder(): GEnumEntryLabelBuilder {
        return new GEnumEntryLabelBuilder(GEnumEntryLabel).type(MetamodelElementType.LABEL_ENUM_ENTRY);
    }
}

/**
 * Builder for GEnumEntryLabel instances.
 * Provides fluent API for constructing enum entry labels.
 */
export class GEnumEntryLabelBuilder<T extends GEnumEntryLabel = GEnumEntryLabel> extends GLabelBuilder<T> {}
