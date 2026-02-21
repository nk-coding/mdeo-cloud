import { sharedImport } from "../../sharedImport.js";
import { HiddenBoundsUpdater } from "./hiddenBoundsUpdater.js";
import { ResetCanvasBoundsCommand } from "./resetCanvasBoundsCommand.js";
import { UpdateModelBoundsActionHandler } from "./updateModelBoundsActionHandler.js";

const { FeatureModule, configureActionHandler, configureCommand, UpdateModelAction, SetModelAction } =
    sharedImport("@eclipse-glsp/sprotty");
const { GLSPHiddenBoundsUpdater } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for bounds-related functionalities.
 * Provides custom implementations for:
 * - HiddenBoundsUpdater: Handles bounds computation for foreign objects
 * - ResetCanvasBoundsCommand: Handles canvas bounds reset on resize
 */
export const boundsModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        bind(HiddenBoundsUpdater).toSelf().inSingletonScope();
        rebind(GLSPHiddenBoundsUpdater).toService(HiddenBoundsUpdater);

        configureCommand({ bind, isBound }, ResetCanvasBoundsCommand);
        configureActionHandler({ bind, isBound }, UpdateModelAction.KIND, UpdateModelBoundsActionHandler);
        configureActionHandler({ bind, isBound }, SetModelAction.KIND, UpdateModelBoundsActionHandler);
    },
    { featureId: Symbol("bounds") }
);
