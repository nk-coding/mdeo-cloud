import { sharedImport, Toolbox, CreateEdgeContextProvider } from "@mdeo/editor-shared";
import { MetamodelToolbox } from "./metamodelToolbox.js";
import { MetamodelCreateEdgeContextProvider } from "./createEdgeContextProvider.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that replaces the base Toolbox with MetamodelToolbox and
 * registers the metamodel-specific create-edge context provider.
 * Must be included after the base toolboxModule (from DEFAULT_MODULES) so the
 * rebind takes effect.
 */
export const metamodelToolboxModule = new FeatureModule(
    (bind, _unbind, _isBound, rebind) => {
        rebind(Toolbox).to(MetamodelToolbox).inSingletonScope();
        bind(CreateEdgeContextProvider).to(MetamodelCreateEdgeContextProvider).inSingletonScope();
    },
    { featureId: Symbol("metamodelToolbox") }
);
