import { sharedImport } from "../../sharedImport.js";
import { ContainerManager } from "./containerManager.js";

const { FeatureModule, TYPES } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the create node tool.
 * Customizes containment logic
 */
export const createNodeToolModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
       bind(ContainerManager).toSelf().inSingletonScope();
       rebind(TYPES.IContainerManager).toService(ContainerManager);
    },
    { featureId: Symbol("create-node-tool") }
);
