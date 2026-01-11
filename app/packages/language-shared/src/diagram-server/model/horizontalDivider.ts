import { sharedImport } from "../../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A horizontal divider element for separating sections.
 */
export class GHorizontalDivider extends GModelElement {}

export class GHorizontalDividerBuilder<
    T extends GHorizontalDivider = GHorizontalDivider
> extends GModelElementBuilder<T> {}
