import { sharedImport } from "../sharedImport.js";

const { GShapeElement, GShapeElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A vertical divider element for separating sections.
 */
export class GVerticalDivider extends GShapeElement {}

export class GVerticalDividerBuilder<T extends GVerticalDivider = GVerticalDivider> extends GShapeElementBuilder<T> {}
