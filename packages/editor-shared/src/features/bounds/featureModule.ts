import type { ContainerModule } from "inversify";
import { sharedImport } from "../../sharedImport.js";
import { HiddenBoundsUpdater } from "./hiddenBoundsUpdater.js";
import { SetBoundsFeedbackCommand } from "./setBoundsFeedbackCommand.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/sprotty");
const { GLSPHiddenBoundsUpdater, SetBoundsFeedbackCommand: GLSPSetBoundsFeedbackCommand } =
    sharedImport("@eclipse-glsp/client");

/**
 * Feature module for bounds-related functionalities.
 * Provides custom implementations for:
 * - HiddenBoundsUpdater: Handles bounds computation for foreign objects
 * - SetBoundsFeedbackCommand: Stores preferred size in metadata
 */
export const boundsModule: ContainerModule = new FeatureModule((bind, unbind, isBound, rebind) => {
    bind(HiddenBoundsUpdater).toSelf().inSingletonScope();
    rebind(GLSPHiddenBoundsUpdater).toService(HiddenBoundsUpdater);
    bind(SetBoundsFeedbackCommand).toSelf();
    rebind(GLSPSetBoundsFeedbackCommand).toService(SetBoundsFeedbackCommand);
});
