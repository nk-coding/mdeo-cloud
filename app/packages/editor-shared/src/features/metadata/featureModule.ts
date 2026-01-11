import type { ContainerModule } from "inversify";
import { sharedImport } from "../../sharedImport.js";
import { SetBoundsFeedbackCommand } from "../metadata/setBoundsFeedbackCommand.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/sprotty");
const { SetBoundsFeedbackCommand: GLSPSetBoundsFeedbackCommand } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for metadata related functionalities.
 */
export const metadataModule: ContainerModule = new FeatureModule((bind, unbind, isBound, rebind) => {
    bind(SetBoundsFeedbackCommand).toSelf();
    rebind(GLSPSetBoundsFeedbackCommand).toService(SetBoundsFeedbackCommand);
});
