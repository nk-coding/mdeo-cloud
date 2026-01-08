import { GCompartment, GCompartmentBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Compartment for grouping labels within a class node.
 */
export class ClassCompartment extends GCompartment {
    /**
     * Creates a builder for ClassCompartment instances.
     * 
     * @returns A new ClassCompartmentBuilder with vertical box layout
     */
    static override builder(): ClassCompartmentBuilder {
        return new ClassCompartmentBuilder(ClassCompartment).type(MetamodelElementType.COMPARTMENT).layout("vbox");
    }
}

/**
 * Builder for ClassCompartment instances.
 * Provides fluent API for constructing class compartments.
 */
export class ClassCompartmentBuilder<T extends ClassCompartment = ClassCompartment> extends GCompartmentBuilder<T> {}
