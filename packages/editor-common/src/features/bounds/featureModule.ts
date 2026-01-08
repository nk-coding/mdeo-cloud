import type { PluginContext } from "../../plugin/pluginContext.js";
import { createHiddenBoundsUpdater } from "./hiddenBoundsUpdater.js";

/**
 * Creates a feature module for bounds-related functionalities.
 */
export function createBoundsModule(context: PluginContext) {
    const { "@eclipse-glsp/sprotty": glspSprotty, "@eclipse-glsp/client": glspClient } = context;
    const { FeatureModule } = glspSprotty;
    const { GLSPHiddenBoundsUpdater } = glspClient;
    const HiddenBoundsUpdaterImpl = createHiddenBoundsUpdater(context);

    return new FeatureModule((bind, unbind, isBound, rebind) => {
        bind(HiddenBoundsUpdaterImpl).toSelf().inSingletonScope();
        rebind(GLSPHiddenBoundsUpdater).toService(HiddenBoundsUpdaterImpl);
    });
}
