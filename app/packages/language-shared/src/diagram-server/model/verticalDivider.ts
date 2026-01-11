import { sharedImport } from "../../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A vertical divider element for separating sections.
 */
export class GVerticalDivider extends GModelElement {}

export class GVerticalDividerBuilder<T extends GVerticalDivider = GVerticalDivider> extends GModelElementBuilder<T> {}
