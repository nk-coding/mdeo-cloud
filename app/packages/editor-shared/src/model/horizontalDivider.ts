import { sharedImport } from "../sharedImport.js";

const { GChildElement } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for horizontal divider elements.
 * Used to visually separate content horizontally within a container.
 * Configured to grab horizontal space but not vertical space.
 */
export class GHorizontalDivider extends GChildElement {}
