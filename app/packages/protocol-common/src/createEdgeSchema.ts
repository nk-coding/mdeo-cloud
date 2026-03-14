import type { GModelElementSchema } from "@eclipse-glsp/protocol";

/**
 * Schema used during interactive edge creation.
 * Contains the rendered template and backend parameters for insertion.
 */
export interface CreateEdgeSchema {
    /**
     * The edge element type identifier.
     */
    elementTypeId: string;
    /**
     * Template used for feedback edge rendering on the client.
     */
    template: GModelElementSchema;
    /**
     * Opaque backend parameters used to disambiguate creation semantics.
     */
    params: Record<string, any>;
}
