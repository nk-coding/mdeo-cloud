import { sharedImport } from "../../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A vertical divider element for separating sections.
 */
export class GVerticalDivider extends GModelElement {
    /**
     * Creates a builder for GVerticalDivider instances.
     *
     * @returns A new GVerticalDividerBuilder
     */
    static builder(): GVerticalDividerBuilder {
        return new GVerticalDividerBuilder(GVerticalDivider);
    }
}

export class GVerticalDividerBuilder<T extends GVerticalDivider = GVerticalDivider> extends GModelElementBuilder<T> {}
