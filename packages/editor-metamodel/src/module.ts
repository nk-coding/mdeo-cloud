import type { BindingContext } from "@eclipse-glsp/sprotty";
import { sharedImport, CompartmentView, HorizontalDividerView, GHorizontalDivider } from "@mdeo/editor-shared";
import { GClassLabel } from "./model/classLabel.js";
import { GPropertyLabel } from "./model/propertyLabel.js";
import { GAssociationEndLabel } from "./model/associationEndLabel.js";
import { GInheritanceEdge } from "./model/inheritanceEdge.js";
import { GAssociationEdge } from "./model/associationEdge.js";
import { GClassCompartment } from "./model/classCompartment.js";
import { ClassNodeView } from "./views/classNodeView.js";
import { MetamodelElementType } from "./model/elementTypes.js";
import { GClassNode } from "./model/classNode.js";

const { FeatureModule, configureModelElement, GLabelView, PolylineEdgeView, editLabelFeature } =
    sharedImport("@eclipse-glsp/sprotty");
const { configureDefaultModelElements } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the metamodel editor.
 * Configures the metamodel-specific model elements and their views.
 */
export const metamodelDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context: BindingContext = { bind, isBound, unbind, rebind };

        configureDefaultModelElements(context);

        configureModelElement(context, MetamodelElementType.NODE_CLASS, GClassNode, ClassNodeView);

        configureModelElement(context, MetamodelElementType.LABEL_CLASS_NAME, GClassLabel, GLabelView, {
            enable: [editLabelFeature]
        });
        configureModelElement(context, MetamodelElementType.LABEL_PROPERTY, GPropertyLabel, GLabelView);
        configureModelElement(context, MetamodelElementType.LABEL_ASSOCIATION_END, GAssociationEndLabel, GLabelView);

        configureModelElement(context, MetamodelElementType.EDGE_INHERITANCE, GInheritanceEdge, PolylineEdgeView);
        configureModelElement(context, MetamodelElementType.EDGE_ASSOCIATION, GAssociationEdge, PolylineEdgeView);

        configureModelElement(context, MetamodelElementType.COMPARTMENT, GClassCompartment, CompartmentView);
        configureModelElement(context, MetamodelElementType.DIVIDER, GHorizontalDivider, HorizontalDividerView);
    },
    { featureId: Symbol("metamodelDiagram") }
);
