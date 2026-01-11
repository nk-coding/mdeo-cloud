import { sharedImport } from "../../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A horizontal divider element for separating sections.
 */
export class GCompartment extends GModelElement {}

export class GCompartmentBuilder<T extends GCompartment = GCompartment> extends GModelElementBuilder<T> {}
