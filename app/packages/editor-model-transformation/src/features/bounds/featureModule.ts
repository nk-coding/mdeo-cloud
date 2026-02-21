import { boundsModule, sharedImport } from "@mdeo/editor-shared";
import { ModelTransformationHiddenBoundsUpdater } from "./hiddenBoundsUpdater.js";
import { IterativeRequestBoundsCommand } from "./iterativeRequestBounds.js";

const { FeatureModule, configureCommand } = sharedImport("@eclipse-glsp/sprotty");
const { GLSPHiddenBoundsUpdater } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for model transformation bounds-related functionalities.
 * Configures bounds computation to use two rounds of bounds computation, which is necessary for match nodes
 */
export const modelTransformationBoundsModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        bind(ModelTransformationHiddenBoundsUpdater).toSelf().inSingletonScope();
        rebind(GLSPHiddenBoundsUpdater).toService(ModelTransformationHiddenBoundsUpdater);

        configureCommand({ bind, isBound }, IterativeRequestBoundsCommand);
    },
    { featureId: Symbol("modelTransformationBounds"), requires: [boundsModule] }
);
