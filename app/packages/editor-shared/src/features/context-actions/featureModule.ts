import { sharedImport } from "../../sharedImport.js";
import { ContextActionsUIExtension } from "./contextActions.js";
import { ToolStateManager } from "../tool-state/toolStateManager.js";

const { FeatureModule, configureActionHandler } = sharedImport("@eclipse-glsp/client");
const { SetModelAction, UpdateModelAction, TYPES } = sharedImport("@eclipse-glsp/sprotty");
const { TYPES: clientTYPES } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that registers the unified {@link ContextActionsUIExtension}
 * singleton and wires it up as an action handler, a diagram startup hook, and
 * an `IVNodePostprocessor` so the rail overlay is refreshed on every render
 * frame (including mid-animation and mid-drag frames).
 *
 * Requires {@link ToolStateManager} to be present in the container (provided by
 * the {@link toolStateModule}) so the rail can be suppressed during creation modes.
 */
export const contextActionsModule = new FeatureModule(
    (bind, _unbind, isBound) => {
        bind(ContextActionsUIExtension).toSelf().inSingletonScope();
        bind(clientTYPES.IDiagramStartup).toService(ContextActionsUIExtension);
        bind(TYPES.IVNodePostprocessor).toService(ContextActionsUIExtension);

        const context = { bind, isBound };

        configureActionHandler(context, SetModelAction.KIND, ContextActionsUIExtension);
        configureActionHandler(context, UpdateModelAction.KIND, ContextActionsUIExtension);
    },
    { featureId: Symbol("contextActions") }
);
