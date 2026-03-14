import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "@mdeo/protocol-metamodel";

/**
 * Label for association property information.
 * The label inside a GAssociationPropertyNode node.
 */
export class GAssociationPropertyLabel extends GLabel {
    /**
     * Creates a builder for GAssociationPropertyLabel instances.
     *
     * @returns A new GAssociationPropertyLabelBuilder
     */
    static builder(): GAssociationPropertyLabelBuilder {
        return new GAssociationPropertyLabelBuilder(GAssociationPropertyLabel).type(
            MetamodelElementType.LABEL_ASSOCIATION_PROPERTY
        );
    }
}

/**
 * Builder for GAssociationPropertyLabel instances.
 * Provides fluent API for constructing association property labels.
 */
export class GAssociationPropertyLabelBuilder<
    T extends GAssociationPropertyLabel = GAssociationPropertyLabel
> extends GLabelBuilder<T> {}
