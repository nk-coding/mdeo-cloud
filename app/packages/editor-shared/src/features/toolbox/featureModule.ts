import type { ContainerModule } from "inversify";
import { sharedImport } from "../../sharedImport.js";
import { Toolbox } from "./toolbox.js";
import { ToolState } from "./toolState.js";
import { DefaultPreviewRenderer, PreviewRenderer } from "./previewRenderer.js";

const { FeatureModule, configureActionHandler } = sharedImport("@eclipse-glsp/client");
const { SetModelAction, UpdateModelAction } = sharedImport("@eclipse-glsp/sprotty");
const { EnableDefaultToolsAction, ToolPalette } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that provides the Toolbox UI extension.
 * Registers the Toolbox as a UI extension and action handler for model updates.
 */
export const toolboxModule: ContainerModule = new FeatureModule((bind, _unbind, isBound, rebind) => {
    bind(ToolState).toSelf().inSingletonScope();

    bind(Toolbox).toSelf().inSingletonScope();
    rebind(ToolPalette).toService(Toolbox);

    bind(DefaultPreviewRenderer).toSelf().inSingletonScope();
    bind(PreviewRenderer).toService(DefaultPreviewRenderer);

    configureActionHandler({ bind, isBound }, EnableDefaultToolsAction.KIND, Toolbox);
    configureActionHandler({ bind, isBound }, UpdateModelAction.KIND, Toolbox);
    configureActionHandler({ bind, isBound }, SetModelAction.KIND, Toolbox);
});
