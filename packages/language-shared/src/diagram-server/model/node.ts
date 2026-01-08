import { sharedImport } from "../../sharedImport.js";
import type { NodeLayoutMetadata } from "../metadataTypes.js";

const { GNode: GNodeBase, GNodeBuilder: GNodeBuilderBase } = sharedImport("@eclipse-glsp/server");

/**
 * Extended node class with layout metadata support.
 * Adds metadata for node positioning and sizing.
 */
export class GNode extends GNodeBase {
    /** Layout metadata containing position and preferred dimensions */
    meta?: NodeLayoutMetadata;
}

/**
 * Builder for GNode instances.
 * Provides fluent API for constructing nodes with metadata.
 */
export class GNodeBuilder<T extends GNode = GNode> extends GNodeBuilderBase<T> {
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

export { GNodeBase, GNodeBuilderBase };
