import type { EditableLabel, GChildElement, GModelElement } from "@eclipse-glsp/sprotty";
import { sharedImport } from "@mdeo/editor-shared";

const {
    RectangularNode,
    SEdgeImpl,
    GLabel,
    boundsFeature,
    connectableFeature,
    deletableFeature,
    fadeFeature,
    hoverFeedbackFeature,
    layoutContainerFeature,
    moveFeature,
    popupFeature,
    selectFeature,
    nameFeature,
    withEditLabelFeature,
    isEditableLabel
} = sharedImport("@eclipse-glsp/sprotty");

/**
 * Type constants for metamodel diagram elements.
 * These must match the server-side MetamodelElementType enum.
 */
export const MetamodelElementType = {
    NODE_CLASS: "node:class",
    LABEL_CLASS_NAME: "label:class-name",
    LABEL_PROPERTY: "label:property",
    LABEL_ASSOCIATION_END: "label:association-end",
    EDGE_INHERITANCE: "edge:inheritance",
    EDGE_ASSOCIATION: "edge:association"
} as const;

/**
 * Client-side model for a Class node.
 * Represents a class in the metamodel diagram with properties displayed as labels.
 */
export class ClassNode extends RectangularNode {
    static override readonly DEFAULT_FEATURES = [
        connectableFeature,
        deletableFeature,
        selectFeature,
        boundsFeature,
        moveFeature,
        layoutContainerFeature,
        fadeFeature,
        hoverFeedbackFeature,
        popupFeature,
        nameFeature,
        withEditLabelFeature
    ];

    name?: string;
    isAbstract?: boolean;

    get editableLabel(): (GChildElement & EditableLabel) | undefined {
        const label = this.children.find((element) => element.type === MetamodelElementType.LABEL_CLASS_NAME);
        if (label && isEditableLabel(label)) {
            return label;
        }
        return undefined;
    }
}

export function isClassNode(element: GModelElement): element is ClassNode {
    return element instanceof ClassNode;
}

/**
 * Client-side model for the Class name label.
 */
export class ClassLabel extends GLabel {}

/**
 * Client-side model for property labels within a Class node.
 */
export class PropertyLabel extends GLabel {}

/**
 * Client-side model for association endpoint labels.
 */
export class AssociationEndLabel extends GLabel {}

/**
 * Client-side model for inheritance edges (extends relationships).
 */
export class InheritanceEdge extends SEdgeImpl {}

/**
 * Client-side model for association edges.
 */
export class AssociationEdge extends SEdgeImpl {
    operator?: string;
}
