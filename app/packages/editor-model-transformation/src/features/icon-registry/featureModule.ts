import { sharedImport, DefaultIconRegistry } from "@mdeo/editor-shared";
import { ModelTransformationIconRegistry } from "./modelTransformationIconRegistry.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that replaces the default {@link DefaultIconRegistry} with the
 * model-transformation-specific {@link ModelTransformationIconRegistry}, which adds
 * custom SVG icons used by the model transformation diagram.
 *
 * Must be included after the base `iconRegistryModule` (from `DEFAULT_MODULES`) so
 * that the `rebind` takes effect.
 */
export const modelTransformationIconRegistryModule = new FeatureModule(
    (_bind, _unbind, _isBound, rebind) => {
        rebind(DefaultIconRegistry).to(ModelTransformationIconRegistry).inSingletonScope();
    },
    { featureId: Symbol("modelTransformationIconRegistry") }
);
