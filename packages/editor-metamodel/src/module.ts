import type { BindingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "@mdeo/editor-shared";
import {
    MetamodelElementType,
    ClassNode,
    ClassLabel,
    PropertyLabel,
    AssociationEndLabel,
    InheritanceEdge,
    AssociationEdge
} from "./model.js";

const { FeatureModule, configureModelElement, GLabelView, RectangularNodeView, PolylineEdgeView, editLabelFeature } =
    sharedImport("@eclipse-glsp/sprotty");
    const { configureDefaultModelElements, DefaultTypes  } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the metamodel editor.
 * Configures the metamodel-specific model elements and their views.
 */
export const metamodelDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context: Pick<BindingContext, "bind" | "isBound"> = { bind, isBound };

        configureDefaultModelElements(context);

        configureModelElement(context, MetamodelElementType.NODE_CLASS, ClassNode, RectangularNodeView);

        configureModelElement(context, MetamodelElementType.LABEL_CLASS_NAME, ClassLabel, GLabelView, {
            enable: [editLabelFeature]
        });
        configureModelElement(context, MetamodelElementType.LABEL_PROPERTY, PropertyLabel, GLabelView);
        configureModelElement(context, MetamodelElementType.LABEL_ASSOCIATION_END, AssociationEndLabel, GLabelView);

        configureModelElement(context, MetamodelElementType.EDGE_INHERITANCE, InheritanceEdge, PolylineEdgeView);
        configureModelElement(context, MetamodelElementType.EDGE_ASSOCIATION, AssociationEdge, PolylineEdgeView);
    },
    { featureId: Symbol("metamodelDiagram") }
);
