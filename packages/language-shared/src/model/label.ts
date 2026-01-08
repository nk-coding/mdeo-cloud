import { sharedImport } from "../sharedImport.js";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * An HTML label element.
 */
export class GHtmlLabel extends GModelElement {
    text?: string;
}

export class GHtmlLabelBuilder<T extends GHtmlLabel = GHtmlLabel> extends GModelElementBuilder<T> {
    text(text: string): this {
        this.proxy.text = text;
        return this;
    }
}
