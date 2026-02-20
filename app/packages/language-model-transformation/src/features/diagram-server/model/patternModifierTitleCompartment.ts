import { GCompartment, GCompartmentBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Compartment that displays a pattern modifier stereotype (e.g. «create», «delete», «forbid»)
 * above the instance name label. Used as the title compartment for pattern instance nodes
 * that have a non-NONE modifier.
 * The modifier kind is not stored here; the client-side view resolves it at render time
 * by traversing up to the parent GPatternInstanceNode.
 */
export class GPatternModifierTitleCompartment extends GCompartment {
    /**
     * Creates a builder for GPatternModifierTitleCompartment instances.
     *
     * @returns A new GPatternModifierTitleCompartmentBuilder
     */
    static override builder(): GPatternModifierTitleCompartmentBuilder {
        return new GPatternModifierTitleCompartmentBuilder(GPatternModifierTitleCompartment).type(
            ModelTransformationElementType.COMPARTMENT_MODIFIER_TITLE
        );
    }
}

/**
 * Builder for GPatternModifierTitleCompartment instances.
 */
export class GPatternModifierTitleCompartmentBuilder<
    T extends GPatternModifierTitleCompartment = GPatternModifierTitleCompartment
> extends GCompartmentBuilder<T> {}
