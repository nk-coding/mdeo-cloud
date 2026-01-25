import { sharedImport } from "../../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A horizontal divider element for separating sections.
 */
export class GHorizontalDivider extends GModelElement {
    /**
     * Creates a builder for GHorizontalDivider instances.
     *
     * @returns A new GHorizontalDividerBuilder
     */
    static builder(): GHorizontalDividerBuilder {
        return new GHorizontalDividerBuilder(GHorizontalDivider);
    }
}

export class GHorizontalDividerBuilder<
    T extends GHorizontalDivider = GHorizontalDivider
> extends GModelElementBuilder<T> {}
