/**
 * Type constants for metamodel diagram elements.
 */
export enum MetamodelElementType {
    NODE_CLASS = "node:class",
    NODE_ENUM = "node:enum",
    NODE_ASSOCIATION_PROPERTY = "node:association-property",
    NODE_ASSOCIATION_MULTIPLICITY = "node:association-multiplicity",
    LABEL_CLASS_NAME = "label:class-name",
    LABEL_ENUM_NAME = "label:enum-name",
    LABEL_ENUM_ENTRY = "label:enum-entry",
    LABEL_PROPERTY = "label:property",
    LABEL_ASSOCIATION_PROPERTY = "label:association-property",
    LABEL_ASSOCIATION_MULTIPLICITY = "label:association-multiplicity",
    EDGE_INHERITANCE = "edge:inheritance",
    EDGE_ASSOCIATION = "edge:association",
    COMPARTMENT = "comp:compartment",
    COMPARTMENT_ENUM_TITLE = "comp:enum-title",
    DIVIDER = "divider:horizontal"
}

/**
 * Enum for association end kinds.
 * Represents the decoration at each end of an association edge.
 */
export enum AssociationEndKind {
    /** No decoration (plain line end) */
    NONE = "none",
    /** Composition (filled diamond) */
    COMPOSITION = "composition",
    /** Navigability (arrow) */
    ARROW = "arrow"
}
