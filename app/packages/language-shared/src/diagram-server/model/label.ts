import { sharedImport } from "../../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * A label element.
 */
export class GLabel extends GModelElement {
    /**
     * The text content of the label
     */
    text!: string;

    /**
     * Indicates if the label is read-only
     */
    readonly?: boolean;
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

    /**
     * Sets whether the label is read-only.
     *
     * @param readonly True if the label should be read-only, false otherwise
     * @returns This builder for chaining
     */
    readonly(readonly: boolean): this {
        this.proxy.readonly = readonly;
        return this;
    }
}
