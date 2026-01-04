import type { EdgeAttributes, NodeAttributes } from "./graph-edit-distance/multiGraph.js";

/**
 * Datastructure which is used to persist metadata for GLSP-based graphical editors
 */
export interface GraphMetadata {
    /**
     * Metadata for nodes in the graph
     * Note that not all nodes need to have metadata, and not each metadata needs to be associated with an actual node
     */
    nodes: Record<string, NodeMetadata>;
    /**
     * Metadata for edges in the graph
     * Note that not all edges need to have metadata, and not each metadata needs to be associated with an actual edge
     */
    edges: Record<string, EdgeMetadata>;
}

/**
 * Metadata for a single node in the graph
 */
export interface NodeMetadata {
    /**
     * The type of the node
     * A metadata for a node with the same type and id is automatically considered to be valid
     */
    type: string;
    /**
     * Attributes used during graph edit distance calculations
     */
    attrs?: NodeAttributes;
    /**
     * The actual metadata object
     */
    meta?: object;
}

/**
 * Metadata for a single edge in the graph
 */
export interface EdgeMetadata {
    /**
     * The type of the edge
     * A metadata for an edge with the same type, source, and target is automatically considered to be valid
     */
    type: string;
    /**
     * The source node ID of the edge
     */
    from: string;
    /**
     * The target node ID of the edge
     */
    to: string;
    /**
     * Attributes used during graph edit distance calculations
     */
    attrs?: EdgeAttributes;
    /**
     * The actual metadata object
     */
    meta?: object;
}