/**
 * Type constants for metamodel diagram elements.
 * These must match the server-side MetamodelElementType enum.
 */
export enum MetamodelElementType {
    NODE_CLASS = "node:class",
    NODE_ASSOCIATION_PROPERTY = "node:association-property",
    NODE_ASSOCIATION_MULTIPLICITY = "node:association-multiplicity",
    LABEL_CLASS_NAME = "label:class-name",
    LABEL_PROPERTY = "label:property",
    LABEL_ASSOCIATION_PROPERTY = "label:association-property",
    LABEL_ASSOCIATION_MULTIPLICITY = "label:association-multiplicity",
    LABEL_ASSOCIATION_END = "label:association-end",
    EDGE_INHERITANCE = "edge:inheritance",
    EDGE_ASSOCIATION = "edge:association",
    COMPARTMENT = "comp:compartment",
    DIVIDER = "divider:horizontal"
}
