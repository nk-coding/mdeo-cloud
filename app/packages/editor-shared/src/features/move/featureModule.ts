import type { ContainerModule } from "inversify";
import { sharedImport } from "../../sharedImport.js";
import { MoveCommand } from "./moveCommand.js";

const { MoveCommand: SprottyMoveCommand } = sharedImport("@eclipse-glsp/sprotty");
const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for move related functionalities.
 */
export const moveModule: ContainerModule = new FeatureModule((bind, unbind, isBound, rebind) => {
    bind(MoveCommand).toSelf();
    rebind(SprottyMoveCommand).toService(MoveCommand);
});
