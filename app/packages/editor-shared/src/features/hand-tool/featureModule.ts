import { sharedImport } from "../../sharedImport.js";
import { HandTool } from "./handTool.js";

const { FeatureModule, TYPES, bindAsService } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the hand tool.
 * Registers HandTool as an on-demand tool (TYPES.ITool).
 * When enabled, all default tools are automatically disabled and selection is suppressed.
 */
export const handToolModule = new FeatureModule(
    (bind) => {
        bindAsService({ bind }, TYPES.ITool, HandTool);
    },
    { featureId: Symbol("hand-tool") }
);
