import { sharedImport } from "../sharedImport.js";

const { GShapeElement, GShapeElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A horizontal divider element for separating sections.
 */
export class GHorizontalDivider extends GShapeElement {}

export class GHorizontalDividerBuilder<
    T extends GHorizontalDivider = GHorizontalDivider
> extends GShapeElementBuilder<T> {}
