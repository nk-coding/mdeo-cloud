import { sharedImport } from "../../sharedImport.js";
import { SetViewportCommand } from "./viewport.js";
import { ZoomMouseListener } from "./zoom.js";
import { SetModelActionHandler } from "./setModelActionHandler.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");
const {
    ZoomMouseListener: SprottyZoomMouseListener,
    SetViewportCommand: SprottySetViewportCommand,
    configureActionHandler,
    SetModelAction,
    configureCommand
} = sharedImport("@eclipse-glsp/sprotty");
const { FitToScreenAction } = sharedImport("@eclipse-glsp/protocol");

/**
 * Feature module for extended viewport functionality
 */
export const viewportModule = new FeatureModule((bind, unbind, isBound, rebind) => {
    bind(ZoomMouseListener).toSelf().inSingletonScope();
    rebind(SprottyZoomMouseListener).toService(ZoomMouseListener);
    bind(SetViewportCommand).toSelf();
    rebind(SprottySetViewportCommand).toService(SetViewportCommand);

    configureActionHandler({ bind, isBound }, SetModelAction.KIND, SetModelActionHandler);
});
