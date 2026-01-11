import { sharedImport } from "../../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A label element.
 */
export class GLabel extends GModelElement {
    /** The text content of the label */
    text?: string;
}

/**
 * Builder for GLabel instances.
 * Provides fluent API for constructing labels.
 */
export class GLabelBuilder<T extends GLabel = GLabel> extends GModelElementBuilder<T> {
    /**
     * Sets the text content of the label.
     *
     * @param text The text to display
     * @returns This builder for chaining
     */
    text(text: string): this {
        this.proxy.text = text;
        return this;
    }
}
