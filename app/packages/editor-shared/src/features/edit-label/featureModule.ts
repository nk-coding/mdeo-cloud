import type { ContainerModule } from "inversify";
import { sharedImport } from "../../sharedImport.js";
import { EditLabelTool } from "./editLabelTool.js";
import { UpdateLabelEditCommand } from "./updateLabelEditAction.js";

const { FeatureModule, configureCommand } = sharedImport("@eclipse-glsp/sprotty");
const { DirectLabelEditTool } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for edit label functionality.
 */
export const editLabelModule: ContainerModule = new FeatureModule((bind, unbind, isBound, rebind) => {
    bind(EditLabelTool).toSelf().inSingletonScope();
    rebind(DirectLabelEditTool).toService(EditLabelTool);
    configureCommand({ bind, isBound }, UpdateLabelEditCommand);
});
