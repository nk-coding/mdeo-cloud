import { sharedImport } from "../../sharedImport.js";
import { HandAwareSelectMouseListener } from "./selectMouseListener.js";

const {
    FeatureModule,
    configureActionHandler,
    RankedSelectMouseListener,
    EnableToolsAction,
    EnableDefaultToolsAction
} = sharedImport("@eclipse-glsp/client");

/**
 * Feature module for select behavior.
 * Replaces RankedSelectMouseListener with HandAwareSelectMouseListener, which
 * suppresses selection while the hand tool is active.
 */
export const selectModule = new FeatureModule(
    (bind, _unbind, isBound, rebind) => {
        bind(HandAwareSelectMouseListener).toSelf().inSingletonScope();
        rebind(RankedSelectMouseListener).toService(HandAwareSelectMouseListener);
        const context = { bind, isBound };
        configureActionHandler(context, EnableToolsAction.KIND, HandAwareSelectMouseListener);
        configureActionHandler(context, EnableDefaultToolsAction.KIND, HandAwareSelectMouseListener);
    },
    { featureId: Symbol("select") }
);
