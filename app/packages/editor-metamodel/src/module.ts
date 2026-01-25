import type { BindingContext } from "@eclipse-glsp/sprotty";
import {
    sharedImport,
    GCompartmentView,
    GHorizontalDividerView,
    GHorizontalDivider,
    GLabelView,
    configureDefaultModelElements,
    GCompartment
} from "@mdeo/editor-shared";
import { GClassLabel } from "./model/classLabel.js";
import { GPropertyLabel } from "./model/propertyLabel.js";
import { GAssociationEndLabel } from "./model/associationEndLabel.js";
import { GAssociationPropertyNode } from "./model/associationPropertyNode.js";
import { GAssociationMultiplicityNode } from "./model/associationMultiplicityNode.js";
import { GAssociationPropertyLabel } from "./model/associationPropertyLabel.js";
import { GAssociationMultiplicityLabel } from "./model/associationMultiplicityLabel.js";
import { GInheritanceEdge } from "./model/inheritanceEdge.js";
import { GAssociationEdge } from "./model/associationEdge.js";
import { GClassNodeView } from "./views/classNodeView.js";
import { GClassLabelView } from "./views/classLabelView.js";
import { GInheritanceEdgeView } from "./views/inheritanceEdgeView.js";
import { GAssociationEdgeView } from "./views/associationEdgeView.js";
import { GAssociationPropertyNodeView } from "./views/associationPropertyNodeView.js";
import { GAssociationMultiplicityNodeView } from "./views/associationMultiplicityNodeView.js";
import { MetamodelElementType } from "./model/elementTypes.js";
import { GClassNode } from "./model/classNode.js";
import { GEnumNode } from "./model/enumNode.js";
import { GEnumLabel } from "./model/enumLabel.js";
import { GEnumEntryLabel } from "./model/enumEntryLabel.js";
import { GEnumNodeView } from "./views/enumNodeView.js";
import { GEnumLabelView } from "./views/enumLabelView.js";
import { GEnumTitleCompartment } from "./model/enumTitleCompartment.js";
import { GEnumTitleCompartmentView } from "./views/enumTitleCompartmentView.js";

const { FeatureModule, configureModelElement, editLabelFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Feature module for the metamodel editor.
 * Configures the metamodel-specific model elements and their views.
 */
export const metamodelDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context: BindingContext = { bind, isBound, unbind, rebind };

        configureDefaultModelElements(context);

        configureModelElement(context, MetamodelElementType.NODE_CLASS, GClassNode, GClassNodeView);
        configureModelElement(context, MetamodelElementType.NODE_ENUM, GEnumNode, GEnumNodeView);
        configureModelElement(
            context,
            MetamodelElementType.NODE_ASSOCIATION_PROPERTY,
            GAssociationPropertyNode,
            GAssociationPropertyNodeView
        );
        configureModelElement(
            context,
            MetamodelElementType.NODE_ASSOCIATION_MULTIPLICITY,
            GAssociationMultiplicityNode,
            GAssociationMultiplicityNodeView
        );

        configureModelElement(context, MetamodelElementType.LABEL_CLASS_NAME, GClassLabel, GClassLabelView, {
            enable: [editLabelFeature]
        });
        configureModelElement(context, MetamodelElementType.LABEL_ENUM_NAME, GEnumLabel, GEnumLabelView, {
            enable: [editLabelFeature]
        });
        configureModelElement(context, MetamodelElementType.LABEL_ENUM_ENTRY, GEnumEntryLabel, GLabelView, {
            enable: [editLabelFeature]
        });
        configureModelElement(context, MetamodelElementType.LABEL_PROPERTY, GPropertyLabel, GLabelView);
        configureModelElement(context, MetamodelElementType.LABEL_ASSOCIATION_END, GAssociationEndLabel, GLabelView);
        configureModelElement(
            context,
            MetamodelElementType.LABEL_ASSOCIATION_PROPERTY,
            GAssociationPropertyLabel,
            GLabelView
        );
        configureModelElement(
            context,
            MetamodelElementType.LABEL_ASSOCIATION_MULTIPLICITY,
            GAssociationMultiplicityLabel,
            GLabelView
        );

        configureModelElement(context, MetamodelElementType.EDGE_INHERITANCE, GInheritanceEdge, GInheritanceEdgeView);
        configureModelElement(context, MetamodelElementType.EDGE_ASSOCIATION, GAssociationEdge, GAssociationEdgeView);
        configureModelElement(
            context,
            MetamodelElementType.COMPARTMENT_ENUM_TITLE,
            GEnumTitleCompartment,
            GEnumTitleCompartmentView
        );

        configureModelElement(context, MetamodelElementType.COMPARTMENT, GCompartment, GCompartmentView);
        configureModelElement(context, MetamodelElementType.DIVIDER, GHorizontalDivider, GHorizontalDividerView);
    },
    { featureId: Symbol("metamodelDiagram") }
);
