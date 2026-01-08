import { sharedImport } from "../sharedImport.js";

const { GChildElement } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for vertical divider elements.
 * Used to visually separate content vertically within a container.
 * Configured to grab vertical space but not horizontal space.
 */
export class GVerticalDivider extends GChildElement {}
