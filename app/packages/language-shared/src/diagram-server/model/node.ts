import { sharedImport } from "../../sharedImport.js";
import type { NodeLayoutMetadata } from "@mdeo/protocol-common";

const { GModelElement, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * Extended node class with layout metadata support.
 * Adds metadata for node positioning and sizing.
 */
export class GNode extends GModelElement {
    /**
     * Layout metadata containing position and preferred dimensions
     */
    meta!: NodeLayoutMetadata;
}

/**
 * Builder for GNode instances.
 * Provides fluent API for constructing nodes with metadata.
 */
export class GNodeBuilder<T extends GNode = GNode> extends GModelElementBuilder<T> {
    /**
     * Sets the layout metadata for the node.
     *
     * @param meta The layout metadata containing position and dimensions
     * @returns This builder for chaining
     */
    meta(meta: NodeLayoutMetadata): this {
        this.proxy.meta = meta;
        return this;
    }
}
