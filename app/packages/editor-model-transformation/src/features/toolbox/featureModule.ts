import { sharedImport, Toolbox } from "@mdeo/editor-shared";
import { ModelTransformationToolbox } from "./modelTransformationToolbox.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the model transformation toolbox.
 * Rebinds the base Toolbox to ModelTransformationToolbox, which adds the
 * node creation mode selector and passes the mode to the server via args.
 */
export const modelTransformationToolboxModule = new FeatureModule(
    (bind, _unbind, _isBound, rebind) => {
        rebind(Toolbox).to(ModelTransformationToolbox).inSingletonScope();
    },
    { featureId: Symbol("modelTransformationToolbox") }
);
