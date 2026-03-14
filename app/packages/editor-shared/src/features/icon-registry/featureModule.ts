import { sharedImport } from "../../sharedImport.js";
import { DefaultIconRegistry } from "./defaultIconRegistry.js";
import { IconRegistryKey } from "./iconRegistry.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that provides context action management and rendering.
 */
export const iconRegistryModule = new FeatureModule(
    (bind) => {
        bind(DefaultIconRegistry).toSelf().inSingletonScope();
        bind(IconRegistryKey).toService(DefaultIconRegistry);
    },
    { featureId: Symbol("iconRegistry") }
);
