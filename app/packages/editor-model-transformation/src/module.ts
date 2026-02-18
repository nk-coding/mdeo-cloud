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
import { ModelTransformationElementType } from "./model/elementTypes.js";
import { GPatternInstanceNode } from "./model/patternInstanceNode.js";
import { GPatternInstanceNameLabel } from "./model/patternInstanceNameLabel.js";
import { GPatternPropertyLabel } from "./model/patternPropertyLabel.js";
import { GPatternLinkEdge } from "./model/patternLinkEdge.js";
import { GPatternLinkEndNode } from "./model/patternLinkEndNode.js";
import { GPatternLinkEndLabel } from "./model/patternLinkEndLabel.js";
import { GStartNode } from "./model/startNode.js";
import { GEndNode } from "./model/endNode.js";
import { GDiamondNode } from "./model/diamondNode.js";
import { GMergeNode } from "./model/mergeNode.js";
import { GControlFlowEdge } from "./model/controlFlowEdge.js";
import { GControlFlowLabelNode } from "./model/controlFlowLabelNode.js";
import { GControlFlowLabel } from "./model/controlFlowLabel.js";
import { GPatternInstanceNodeView } from "./views/patternInstanceNodeView.js";
import { GPatternInstanceLabelView } from "./views/patternInstanceLabelView.js";
import { GPatternPropertyLabelView } from "./views/patternPropertyLabelView.js";
import { GPatternLinkEdgeView } from "./views/patternLinkEdgeView.js";
import { GPatternLinkEndNodeView } from "./views/patternLinkEndNodeView.js";
import { GPatternLinkEndLabelView } from "./views/patternLinkEndLabelView.js";
import { GStartNodeView } from "./views/startNodeView.js";
import { GEndNodeView } from "./views/endNodeView.js";
import { GDiamondNodeView } from "./views/diamondNodeView.js";
import { GMergeNodeView } from "./views/mergeNodeView.js";
import { GControlFlowEdgeView } from "./views/controlFlowEdgeView.js";
import { GControlFlowLabelNodeView } from "./views/controlFlowLabelNodeView.js";
import { GMatchNode } from "./model/matchNode.js";
import { GVariableLabel } from "./model/variableLabel.js";
import { GWhereClauseLabel } from "./model/whereClauseLabel.js";
import { GMatchNodeView } from "./views/matchNodeView.js";

const { FeatureModule, configureModelElement } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Feature module for the model transformation editor.
 * Configures the transformation-specific diagram elements and their views.
 */
export const modelTransformationDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context: BindingContext = { bind, isBound, unbind, rebind };

        configureDefaultModelElements(context);

        // Basic compartment and divider configuration
        configureModelElement(context, ModelTransformationElementType.COMPARTMENT, GCompartment, GCompartmentView);
        configureModelElement(
            context,
            ModelTransformationElementType.DIVIDER,
            GHorizontalDivider,
            GHorizontalDividerView
        );

        // Pattern instance elements
        configureModelElement(
            context,
            ModelTransformationElementType.NODE_PATTERN_INSTANCE,
            GPatternInstanceNode,
            GPatternInstanceNodeView
        );
        configureModelElement(
            context,
            ModelTransformationElementType.LABEL_PATTERN_INSTANCE_NAME,
            GPatternInstanceNameLabel,
            GPatternInstanceLabelView
        );
        configureModelElement(
            context,
            ModelTransformationElementType.LABEL_PATTERN_PROPERTY,
            GPatternPropertyLabel,
            GPatternPropertyLabelView
        );

        // Pattern link elements
        configureModelElement(
            context,
            ModelTransformationElementType.EDGE_PATTERN_LINK,
            GPatternLinkEdge,
            GPatternLinkEdgeView
        );
        configureModelElement(
            context,
            ModelTransformationElementType.NODE_PATTERN_LINK_END,
            GPatternLinkEndNode,
            GPatternLinkEndNodeView
        );
        configureModelElement(
            context,
            ModelTransformationElementType.LABEL_PATTERN_LINK_END,
            GPatternLinkEndLabel,
            GPatternLinkEndLabelView
        );

        // Control flow elements
        configureModelElement(context, ModelTransformationElementType.NODE_START, GStartNode, GStartNodeView);
        configureModelElement(context, ModelTransformationElementType.NODE_END, GEndNode, GEndNodeView);
        configureModelElement(context, ModelTransformationElementType.NODE_DIAMOND, GDiamondNode, GDiamondNodeView);
        configureModelElement(context, ModelTransformationElementType.NODE_MERGE, GMergeNode, GMergeNodeView);
        configureModelElement(
            context,
            ModelTransformationElementType.EDGE_CONTROL_FLOW,
            GControlFlowEdge,
            GControlFlowEdgeView
        );
        configureModelElement(
            context,
            ModelTransformationElementType.NODE_CONTROL_FLOW_LABEL,
            GControlFlowLabelNode,
            GControlFlowLabelNodeView
        );
        configureModelElement(
            context,
            ModelTransformationElementType.LABEL_CONTROL_FLOW,
            GControlFlowLabel,
            GLabelView
        );

        // Match node (container for pattern elements)
        configureModelElement(context, ModelTransformationElementType.NODE_MATCH, GMatchNode, GMatchNodeView);

        // Constraint labels
        configureModelElement(context, ModelTransformationElementType.LABEL_VARIABLE, GVariableLabel, GLabelView);
        configureModelElement(
            context,
            ModelTransformationElementType.LABEL_WHERE_CLAUSE,
            GWhereClauseLabel,
            GLabelView
        );
    },
    { featureId: Symbol("modelTransformationDiagram") }
);
