import type { BindingContext } from "@eclipse-glsp/sprotty";
import {
    sharedImport,
    GCompartmentView,
    GHorizontalDividerView,
    GHorizontalDivider,
    GLabelView,
    configureDefaultModelElements
} from "@mdeo/editor-shared";
import { GClassLabel } from "./model/classLabel.js";
import { GPropertyLabel } from "./model/propertyLabel.js";
import { GAssociationEndLabel } from "./model/associationEndLabel.js";
import { GInheritanceEdge } from "./model/inheritanceEdge.js";
import { GAssociationEdge } from "./model/associationEdge.js";
import { GClassCompartment } from "./model/classCompartment.js";
import { GClassNodeView } from "./views/classNodeView.js";
import { GClassLabelView } from "./views/classLabelView.js";
import { MetamodelElementType } from "./model/elementTypes.js";
import { GClassNode } from "./model/classNode.js";

const { FeatureModule, configureModelElement, PolylineEdgeView, editLabelFeature } =
    sharedImport("@eclipse-glsp/sprotty");

/**
 * Feature module for the metamodel editor.
 * Configures the metamodel-specific model elements and their views.
 */
export const metamodelDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context: BindingContext = { bind, isBound, unbind, rebind };

        configureDefaultModelElements(context);

        configureModelElement(context, MetamodelElementType.NODE_CLASS, GClassNode, GClassNodeView);

        configureModelElement(context, MetamodelElementType.LABEL_CLASS_NAME, GClassLabel, GClassLabelView, {
            enable: [editLabelFeature]
        });
        configureModelElement(context, MetamodelElementType.LABEL_PROPERTY, GPropertyLabel, GLabelView);
        configureModelElement(context, MetamodelElementType.LABEL_ASSOCIATION_END, GAssociationEndLabel, GLabelView);

        configureModelElement(context, MetamodelElementType.EDGE_INHERITANCE, GInheritanceEdge, PolylineEdgeView);
        configureModelElement(context, MetamodelElementType.EDGE_ASSOCIATION, GAssociationEdge, PolylineEdgeView);

        configureModelElement(context, MetamodelElementType.COMPARTMENT, GClassCompartment, GCompartmentView);
        configureModelElement(context, MetamodelElementType.DIVIDER, GHorizontalDivider, GHorizontalDividerView);
    },
    { featureId: Symbol("metamodelDiagram") }
);
