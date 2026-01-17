import { sharedImport } from "../../sharedImport.js";
import { EdgeEditTool } from "./edgeEditTool.js";
import { SetEdgeRoutingFeedbackCommand } from "./edgeRoutingFeedback.js";

const { FeatureModule, configureCommand, bindAsService, TYPES } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the edge edit tool.
 * Provides functionality for editing edge routes by dragging segments.
 */
export const edgeEditToolModule = new FeatureModule((bind, unbind, isBound) => {
    bindAsService({ bind }, TYPES.IDefaultTool, EdgeEditTool);

    configureCommand({ bind, isBound }, SetEdgeRoutingFeedbackCommand);
});
