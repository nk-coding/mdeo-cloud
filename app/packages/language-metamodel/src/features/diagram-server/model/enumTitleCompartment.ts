import { GCompartment, GCompartmentBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "@mdeo/protocol-metamodel";

/**
 * Compartment for grouping labels within an enum title.
 */
export class GEnumTitleCompartment extends GCompartment {
    /**
     * Creates a builder for GEnumTitleCompartment instances.
     *
     * @returns A new GEnumTitleCompartmentBuilder with vertical box layout
     */
    static override builder(): GEnumTitleCompartmentBuilder {
        return new GEnumTitleCompartmentBuilder(GEnumTitleCompartment).type(
            MetamodelElementType.COMPARTMENT_ENUM_TITLE
        );
    }
}

/**
 * Builder for GEnumTitleCompartment instances.
 * Provides fluent API for constructing enum title compartments.
 */
export class GEnumTitleCompartmentBuilder<
    T extends GEnumTitleCompartment = GEnumTitleCompartment
> extends GCompartmentBuilder<T> {}
