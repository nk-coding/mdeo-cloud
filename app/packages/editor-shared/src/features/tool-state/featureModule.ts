import { sharedImport } from "../../sharedImport.js";
import { ToolStateManager } from "./toolStateManager.js";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that registers the {@link ToolStateManager} as a DI singleton.
 *
 * Include this module in your editor container so that both the {@link Toolbox} and
 * components such as {@link ContextActionsUIExtension} can inject it.
 */
export const toolStateModule = new FeatureModule(
    (bind) => {
        bind(ToolStateManager).toSelf().inSingletonScope();
    },
    { featureId: Symbol("tool-state") }
);
