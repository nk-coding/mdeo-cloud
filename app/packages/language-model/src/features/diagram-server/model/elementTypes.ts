/**
 * Type constants for model diagram elements.
 * These must match the client-side ModelElementType enum.
 */
export enum ModelElementType {
    /**
     * Node representing an object instance
     */
    NODE_OBJECT = "node:object",
    /**
     * Label displaying the object's combined name and type (format: "name : type")
     */
    LABEL_OBJECT_NAME = "label:object-name",
    /**
     * Label displaying a property assignment
     */
    LABEL_PROPERTY = "label:property",
    /**
     * Edge representing a link between objects
     */
    EDGE_LINK = "edge:link",
    /**
     * Compartment container for grouping elements
     */
    COMPARTMENT = "comp:compartment",
    /**
     * Horizontal divider line
     */
    DIVIDER = "divider:horizontal",
    /**
     * Node wrapping a link end label (source or target)
     */
    NODE_LINK_END = "node:link-end",
    /**
     * Label at the end of a link (source or target)
     */
    LABEL_LINK_END = "label:link-end",
}
