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
import { GLinkEndNode } from "./model/linkEndNode.js";
import { GLinkEndLabel } from "./model/linkEndLabel.js";
import { GObjectNodeView } from "./views/objectNodeView.js";
import { GObjectLabelView } from "./views/objectLabelView.js";
import { GPropertyValueLabelView } from "./views/propertyValueLabelView.js";
import { GLinkEdgeView } from "./views/linkEdgeView.js";
import { GLinkEndNodeView } from "./views/linkEndNodeView.js";
import { ModelElementType } from "@mdeo/protocol-model";

const { FeatureModule, configureModelElement, editLabelFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Feature module for the model editor.
 * Configures the model-specific diagram elements and their views.
 */
export const modelDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context: BindingContext = { bind, isBound, unbind, rebind };

        configureDefaultModelElements(context);

        configureModelElement(context, ModelElementType.NODE_OBJECT, GObjectNode, GObjectNodeView);

        configureModelElement(context, ModelElementType.LABEL_OBJECT_NAME, GObjectLabel, GObjectLabelView, {
            enable: [editLabelFeature]
        });
        configureModelElement(context, ModelElementType.LABEL_PROPERTY, GPropertyLabel, GPropertyValueLabelView);

        configureModelElement(context, ModelElementType.EDGE_LINK, GLinkEdge, GLinkEdgeView);

        configureModelElement(context, ModelElementType.NODE_LINK_END, GLinkEndNode, GLinkEndNodeView);
        configureModelElement(context, ModelElementType.LABEL_LINK_END, GLinkEndLabel, GLabelView);

        configureModelElement(context, ModelElementType.COMPARTMENT, GCompartment, GCompartmentView);
        configureModelElement(context, ModelElementType.DIVIDER, GHorizontalDivider, GHorizontalDividerView);
    },
    { featureId: Symbol("modelDiagram") }
);
