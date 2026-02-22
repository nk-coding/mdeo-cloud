/**
 * Type constants for model transformation diagram elements.
 * These must match the client-side element types.
 */
export enum ModelTransformationElementType {

    /**
     * Start node of the transformation
     */
    NODE_START = "node:start",

    /**
     * End node (stop or kill)
     */
    NODE_END = "node:end",

    /**
     * Match node containing pattern elements
     */
    NODE_MATCH = "node:match",

    /**
     * Split node for if/while expression branching
     */
    NODE_SPLIT = "node:split",

    /**
     * Merge node where branches join back together
     */
    NODE_MERGE = "node:merge",

    /**
     * Control flow edge connecting nodes
     */
    EDGE_CONTROL_FLOW = "edge:control-flow",

    /**
     * Node wrapping a control flow edge label
     */
    NODE_CONTROL_FLOW_LABEL = "node:control-flow-label",

    /**
     * Label on a control flow edge
     */
    LABEL_CONTROL_FLOW = "label:control-flow",

    /**
     * Pattern object instance node
     */
    NODE_PATTERN_INSTANCE = "node:pattern-instance",

    /**
     * Label displaying the pattern instance name and optional type
     */
    LABEL_PATTERN_INSTANCE_NAME = "label:pattern-instance-name",

    /**
     * Label displaying a property assignment in a pattern
     */
    LABEL_PATTERN_PROPERTY = "label:pattern-property",

    /**
     * Edge representing a pattern link between instances
     */
    EDGE_PATTERN_LINK = "edge:pattern-link",

    /**
     * Node wrapping a pattern link end label
     */
    NODE_PATTERN_LINK_END = "node:pattern-link-end",

    /**
     * Label at the end of a pattern link
     */
    LABEL_PATTERN_LINK_END = "label:pattern-link-end",

    /**
     * Label displaying a where clause condition
     */
    LABEL_WHERE_CLAUSE = "label:where-clause",

    /**
     * Label displaying a variable declaration
     */
    LABEL_VARIABLE = "label:variable",

    /**
     * Modifier label shown in the middle of a pattern link edge (create/delete/forbid)
     */
    LABEL_PATTERN_LINK_MODIFIER = "label:pattern-link-modifier",

    /**
     * Horizontal divider line between compartments
     */
    DIVIDER = "divider:horizontal",

    /**
     * Compartment container for grouping elements
     */
    COMPARTMENT = "comp:compartment",

    /**
     * Container wrapping the bottom compartments of a match node (where-clauses, variables).
     * Includes the horizontal dividers between compartments.
     */
    MATCH_NODE_COMPARTMENTS = "comp:match-node-compartments",

    /**
     * Modifier title compartment (renders «create», «delete», «forbid» + name label)
     */
    COMPARTMENT_MODIFIER_TITLE = "comp:modifier-title"
}

/**
 * Enum for pattern modifier kinds.
 * Represents the create/delete/forbid modifier on pattern elements.
 */
export enum PatternModifierKind {
    /**
     * No modifier (match only)
     */
    NONE = "none",
    /**
     * Create the element
     */
    CREATE = "create",
    /**
     * Delete the element
     */
    DELETE = "delete",
    /**
     * Forbid the element (negative application condition)
     */
    FORBID = "forbid"
}

/**
 * Enum for end node kinds.
 * Represents whether this is a stop or kill termination.
 */
export enum EndNodeKind {
    /**
     * Stop execution normally
     */
    STOP = "stop",
    /**
     * Kill execution (terminate immediately)
     */
    KILL = "kill"
}
