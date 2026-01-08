import { sharedImport } from "../sharedImport.js";

const { GChildElement } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for compartment elements.
 * Compartments are used to group and organize child elements within a node.
 */
export class GCompartment extends GChildElement {}
