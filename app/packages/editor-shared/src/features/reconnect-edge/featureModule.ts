import type { ContainerModule } from "inversify";
import { sharedImport } from "../../sharedImport.js";
import { UpdateReconnectEdgeCommand } from "./updateReconnectEdgeAction.js";

const { FeatureModule, configureCommand } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Feature module for reconnect edge functionality.
 */
export const reconnectEdgeModule: ContainerModule = new FeatureModule(
    (bind, unbind, isBound) => {
        configureCommand({ bind, isBound }, UpdateReconnectEdgeCommand);
    },
    { featureId: Symbol("reconnect-edge") }
);
