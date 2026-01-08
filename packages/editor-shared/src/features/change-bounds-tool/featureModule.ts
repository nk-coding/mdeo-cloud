import { sharedImport } from "../../sharedImport.js";
import { ChangeBoundsTool } from "./changeBoundsTool.js";

const { FeatureModule, ChangeBoundsTool: GLSPChangeBoundsTool } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the change bounds tool.
 * Provides a custom change bounds tool implementation that supports:
 * - Custom SVG-based resize handles with data attributes
 * - Separation of move and resize operations
 */
export const changeBoundsToolModule = new FeatureModule((bind, unbind, isBound, rebind) => {
    bind(ChangeBoundsTool).toSelf().inSingletonScope();
    rebind(GLSPChangeBoundsTool).toService(ChangeBoundsTool);
});
