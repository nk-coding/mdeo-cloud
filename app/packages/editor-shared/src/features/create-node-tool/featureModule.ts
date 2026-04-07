import { sharedImport } from "../../sharedImport.js";
import { ContainerManager } from "./containerManager.js";
import { NodeCreationTool } from "./nodeCreationTool.js";

const { FeatureModule, TYPES } = sharedImport("@eclipse-glsp/client");
const { NodeCreationTool: GLSPNodeCreationTool } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for the create-node tool.
 *
 * - Replaces the default {@link ContainerManager} with a custom version that considers
 *   all elements at the drop position as potential containers.
 * - Replaces the GLSP {@link NodeCreationTool} with {@link NodeCreationTool} which
 *   triggers node creation on mouse-up regardless of incidental cursor movement.
 */
export const createNodeToolModule = new FeatureModule(
    (bind, _unbind, isBound, rebind) => {
        bind(ContainerManager).toSelf().inSingletonScope();
        rebind(TYPES.IContainerManager).toService(ContainerManager);

        bind(NodeCreationTool).toSelf().inSingletonScope();
        rebind(GLSPNodeCreationTool).toService(NodeCreationTool);
    },
    { featureId: Symbol("create-node-tool") }
);
