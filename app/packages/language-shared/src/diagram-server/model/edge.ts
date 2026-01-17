import { sharedImport } from "../../sharedImport.js";
import type { EdgeLayoutMetadata } from "@mdeo/editor-protocol";
import type { GModelElement } from "@eclipse-glsp/server";

const { GModelElement: GModelElementBase, GModelElementBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * Extended edge class with layout metadata support.
 * Adds metadata for edge routing and connection information.
 */
export class GEdge extends GModelElementBase {
    /**
     * Layout metadata containing routing points
     */
    meta!: EdgeLayoutMetadata;

    /**
     * The ID of the source element this edge connects from
     */
    sourceId!: string;

    /**
     * The ID of the target element this edge connects to
     */
    targetId!: string;
}

/**
 * Builder for GEdge instances.
 * Provides fluent API for constructing edges with metadata and connections.
 */
export class GEdgeBuilder<T extends GEdge = GEdge> extends GModelElementBuilder<T> {
    /**
     * Sets the layout metadata for the edge.
     *
     * @param meta The layout metadata containing routing points
     * @returns This builder for chaining
     */
    meta(meta: EdgeLayoutMetadata): this {
        this.proxy.meta = meta;
        return this;
    }

    /**
     * Sets the source ID for the edge.
     *
     * @param sourceId The ID of the source element
     * @returns This builder for chaining
     */
    sourceId(sourceId: string): this {
        this.proxy.sourceId = sourceId;
        return this;
    }

    /**
     * Sets the source element for the edge by extracting its ID.
     *
     * @param source The source GModelElement
     * @returns This builder for chaining
     */
    source(source: GModelElement): this {
        this.proxy.sourceId = source.id;
        return this;
    }

    /**
     * Sets the target ID for the edge.
     *
     * @param targetId The ID of the target element
     * @returns This builder for chaining
     */
    targetId(targetId: string): this {
        this.proxy.targetId = targetId;
        return this;
    }

    /**
     * Sets the target element for the edge by extracting its ID.
     *
     * @param target The target GModelElement
     * @returns This builder for chaining
     */
    target(target: GModelElement): this {
        this.proxy.targetId = target.id;
        return this;
    }
}
