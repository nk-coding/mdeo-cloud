import type { Dimension, LayoutOperation as GLSPLayoutOperation } from "@eclipse-glsp/protocol";

/**
 * Extended layout operation providing bounds information for all elements
 */
export interface LayoutOperation extends GLSPLayoutOperation {
    /**
     * The bounds of all elements in the model
     */
    bounds: Record<string, Dimension>;
}
