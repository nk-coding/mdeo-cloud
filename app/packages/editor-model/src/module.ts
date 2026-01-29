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
import { GObjectNode } from "./model/objectNode.js";
import { GObjectLabel } from "./model/objectLabel.js";
import { GPropertyLabel } from "./model/propertyLabel.js";
import { GLinkEdge } from "./model/linkEdge.js";
import { GLinkSourceLabel } from "./model/linkSourceLabel.js";
import { GLinkTargetLabel } from "./model/linkTargetLabel.js";
import { GObjectNodeView } from "./views/objectNodeView.js";
import { GObjectLabelView } from "./views/objectLabelView.js";
import { GLinkEdgeView } from "./views/linkEdgeView.js";
import { ModelElementType } from "./model/elementTypes.js";

const { FeatureModule, configureModelElement, editLabelFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Feature module for the model editor.
 * Configures the model-specific diagram elements and their views.
 */
export const modelDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context: BindingContext = { bind, isBound, unbind, rebind };

        configureDefaultModelElements(context);

        // Configure object node
        configureModelElement(context, ModelElementType.NODE_OBJECT, GObjectNode, GObjectNodeView);

        // Configure object label (combined name and type)
        configureModelElement(context, ModelElementType.LABEL_OBJECT_NAME, GObjectLabel, GObjectLabelView, {
            enable: [editLabelFeature]
        });
        configureModelElement(context, ModelElementType.LABEL_PROPERTY, GPropertyLabel, GLabelView);

        // Configure link edge
        configureModelElement(context, ModelElementType.EDGE_LINK, GLinkEdge, GLinkEdgeView);

        // Configure link endpoint labels
        configureModelElement(context, ModelElementType.LABEL_LINK_SOURCE, GLinkSourceLabel, GLabelView);
        configureModelElement(context, ModelElementType.LABEL_LINK_TARGET, GLinkTargetLabel, GLabelView);

        // Configure compartments and dividers
        configureModelElement(context, ModelElementType.COMPARTMENT, GCompartment, GCompartmentView);
        configureModelElement(context, ModelElementType.DIVIDER, GHorizontalDivider, GHorizontalDividerView);
    },
    { featureId: Symbol("modelDiagram") }
);
