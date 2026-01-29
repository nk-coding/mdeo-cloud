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
     * @deprecated Use LABEL_OBJECT_NAME which now displays both name and type
     */
    LABEL_OBJECT_TYPE = "label:object-type",
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
     * Label at the source end of a link
     */
    LABEL_LINK_SOURCE = "label:link-source",
    /**
     * Label at the target end of a link
     */
    LABEL_LINK_TARGET = "label:link-target"
}
