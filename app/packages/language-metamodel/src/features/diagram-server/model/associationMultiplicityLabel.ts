import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Label for association multiplicity information.
 * The label inside a GAssociationMultiplicityNode node.
 */
export class GAssociationMultiplicityLabel extends GLabel {
    /**
     * Creates a builder for GAssociationMultiplicityLabel instances.
     *
     * @returns A new GAssociationMultiplicityLabelBuilder
     */
    static builder(): GAssociationMultiplicityLabelBuilder {
        return new GAssociationMultiplicityLabelBuilder(GAssociationMultiplicityLabel).type(
            MetamodelElementType.LABEL_ASSOCIATION_MULTIPLICITY
        );
    }
}

/**
 * Builder for GAssociationMultiplicityLabel instances.
 * Provides fluent API for constructing association multiplicity labels.
 */
export class GAssociationMultiplicityLabelBuilder<
    T extends GAssociationMultiplicityLabel = GAssociationMultiplicityLabel
> extends GLabelBuilder<T> {}
