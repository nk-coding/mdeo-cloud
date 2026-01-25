import { sharedImport } from "../../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A horizontal divider element for separating sections.
 */
export class GCompartment extends GModelElement {
    /**
     * Creates a builder for GCompartment instances.
     *
     * @returns A new GCompartmentBuilder
     */
    static builder(): GCompartmentBuilder {
        return new GCompartmentBuilder(GCompartment);
    }
}

export class GCompartmentBuilder<T extends GCompartment = GCompartment> extends GModelElementBuilder<T> {}
