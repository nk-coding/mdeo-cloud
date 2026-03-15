import { sharedImport, DefaultIconRegistry } from "@mdeo/editor-shared";
import { MetamodelIconRegistry } from "./metamodelIconRegistry.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that replaces the default {@link DefaultIconRegistry} with the
 * metamodel-specific {@link MetamodelIconRegistry}, which adds custom SVG icons
 * for association ends and composition decorators.
 *
 * Must be included after the base `iconRegistryModule` (from `DEFAULT_MODULES`) so
 * that the `rebind` takes effect.
 */
export const metamodelIconRegistryModule = new FeatureModule(
    (_bind, _unbind, _isBound, rebind) => {
        rebind(DefaultIconRegistry).to(MetamodelIconRegistry).inSingletonScope();
    },
    { featureId: Symbol("metamodelIconRegistry") }
);
