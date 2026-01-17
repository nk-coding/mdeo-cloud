import { GCompartment, GCompartmentBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Compartment for grouping labels within a class node.
 */
export class GClassCompartment extends GCompartment {
    /**
     * Creates a builder for GClassCompartment instances.
     *
     * @returns A new GClassCompartmentBuilder with vertical box layout
     */
    static builder(): GClassCompartmentBuilder {
        return new GClassCompartmentBuilder(GClassCompartment).type(MetamodelElementType.COMPARTMENT);
    }
}

/**
 * Builder for GClassCompartment instances.
 * Provides fluent API for constructing class compartments.
 */
export class GClassCompartmentBuilder<T extends GClassCompartment = GClassCompartment> extends GCompartmentBuilder<T> {}
