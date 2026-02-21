import { sharedImport } from "../../sharedImport.js";
import { Toolbox } from "./toolbox.js";
import { ToolState } from "./toolState.js";
import { DefaultPreviewRenderer, PreviewRenderer } from "./previewRenderer.js";
import { LayoutCommand } from "./layoutAction.js";

const { FeatureModule, configureActionHandler, configureCommand } = sharedImport("@eclipse-glsp/client");
const { SetModelAction, UpdateModelAction } = sharedImport("@eclipse-glsp/sprotty");
const { EnableDefaultToolsAction, ToolPalette } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that provides the Toolbox UI extension.
 * Registers the Toolbox as a UI extension and action handler for model updates.
 */
export const toolboxModule = new FeatureModule(
    (bind, _unbind, isBound, rebind) => {
        bind(ToolState).toSelf().inSingletonScope();

        bind(Toolbox).toSelf().inSingletonScope();
        rebind(ToolPalette).toService(Toolbox);

        bind(DefaultPreviewRenderer).toSelf().inSingletonScope();
        bind(PreviewRenderer).toService(DefaultPreviewRenderer);

        const context = { bind, isBound };

        configureActionHandler(context, EnableDefaultToolsAction.KIND, Toolbox);
        configureActionHandler(context, UpdateModelAction.KIND, Toolbox);
        configureActionHandler(context, SetModelAction.KIND, Toolbox);

        configureCommand(context, LayoutCommand);
    },
    { featureId: Symbol("toolbox") }
);
