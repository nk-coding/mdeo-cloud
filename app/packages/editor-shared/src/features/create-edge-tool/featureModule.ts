import { sharedImport } from "../../sharedImport.js";
import { CreateEdgeTool } from "./createEdgeTool.js";
import {
    SetEdgeEditHighlightCommand,
    StartCreateEdgeFeedbackCommand,
    StopCreateEdgeFeedbackCommand,
    UpdateCreateEdgeFeedbackCommand
} from "./createEdgeFeedback.js";
import { CreateEdgeProvider } from "./createEdgeProvider.js";
import { EdgeEditHighlightState } from "../../model/node.js";

const { FeatureModule, configureCommand, bindAsService, TYPES } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the create-edge tool.
 * Registers the on-demand tool and all feedback commands used during
 * the two-phase edge creation flow.
 */
export const createEdgeToolModule = new FeatureModule(
    (bind, unbind, isBound) => {
        bindAsService({ bind }, TYPES.ITool, CreateEdgeTool);
        bind(CreateEdgeProvider).toSelf().inSingletonScope();
        bind(EdgeEditHighlightState).toSelf().inSingletonScope();

        configureCommand({ bind, isBound }, StartCreateEdgeFeedbackCommand);
        configureCommand({ bind, isBound }, UpdateCreateEdgeFeedbackCommand);
        configureCommand({ bind, isBound }, StopCreateEdgeFeedbackCommand);
        configureCommand({ bind, isBound }, SetEdgeEditHighlightCommand);
    },
    { featureId: Symbol("create-edge-tool") }
);
