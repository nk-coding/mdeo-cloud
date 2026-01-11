/**
 * Type constants for metamodel diagram elements.
 * These must match the server-side MetamodelElementType enum.
 */
export enum MetamodelElementType {
    NODE_CLASS = "node:class",
    LABEL_CLASS_NAME = "label:class-name",
    LABEL_PROPERTY = "label:property",
    LABEL_ASSOCIATION_END = "label:association-end",
    EDGE_INHERITANCE = "edge:inheritance",
    EDGE_ASSOCIATION = "edge:association",
    COMPARTMENT = "comp:compartment",
    DIVIDER = "divider:horizontal"
}
