import { type ContainerModule } from "inversify";
import { sharedImport } from "../../sharedImport.js";
import { HiddenBoundsUpdater } from "./hiddenBoundsUpdater.js";
import { ComputedBoundsActionHandler } from "./computedBoundsActionHandler.js";

const { FeatureModule, configureActionHandler, ComputedBoundsAction } = sharedImport("@eclipse-glsp/sprotty");
const { GLSPHiddenBoundsUpdater } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for bounds-related functionalities.
 * Provides custom implementations for:
 * - HiddenBoundsUpdater: Handles bounds computation for foreign objects
 */
export const boundsModule: ContainerModule = new FeatureModule((bind, unbind, isBound, rebind) => {
    bind(HiddenBoundsUpdater).toSelf().inSingletonScope();
    rebind(GLSPHiddenBoundsUpdater).toService(HiddenBoundsUpdater);

    configureActionHandler({bind, isBound}, ComputedBoundsAction.KIND, ComputedBoundsActionHandler)
});
