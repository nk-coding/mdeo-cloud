import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";
import type { EdgePlacement } from "@eclipse-glsp/protocol";

/**
 * Label for association endpoint information (property name and multiplicity).
 */
export class AssociationEndLabel extends GLabel {
    /** The placement of the label along the edge */
    edgePlacement?: EdgePlacement;

    /**
     * Creates a builder for AssociationEndLabel instances.
     *
     * @returns A new AssociationEndLabelBuilder
     */
    static builder(): AssociationEndLabelBuilder {
        return new AssociationEndLabelBuilder(AssociationEndLabel).type(MetamodelElementType.LABEL_ASSOCIATION_END);
    }
}

/**
 * Builder for AssociationEndLabel instances.
 * Provides fluent API for constructing association end labels.
 */
export class AssociationEndLabelBuilder<T extends AssociationEndLabel = AssociationEndLabel> extends GLabelBuilder<T> {}
