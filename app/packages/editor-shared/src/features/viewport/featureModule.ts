import { sharedImport } from "../../sharedImport.js";
import { SetViewportCommand } from "./viewport.js";
import { ZoomMouseListener } from "./zoom.js";
import { InitializeViewportLayoutCommand } from "./initializeViewportLayout.js";
import type { Action, ICommand } from "@eclipse-glsp/sprotty";

const { FeatureModule } = sharedImport("@eclipse-glsp/client");
const {
    ZoomMouseListener: SprottyZoomMouseListener,
    SetViewportCommand: SprottySetViewportCommand,
    SetModelAction,
    UpdateModelAction,
    TYPES
} = sharedImport("@eclipse-glsp/sprotty");
const { Container } = sharedImport("inversify");

/**
 * Feature module for extended viewport functionality
 */
export const viewportModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        bind(ZoomMouseListener).toSelf().inSingletonScope();
        rebind(SprottyZoomMouseListener).toService(ZoomMouseListener);
        bind(SetViewportCommand).toSelf();
        rebind(SprottySetViewportCommand).toService(SetViewportCommand);

        const context = { bind, isBound };
        bind(InitializeViewportLayoutCommand).toSelf();

        for (const actionType of [SetModelAction.KIND, UpdateModelAction.KIND]) {
            context.bind(TYPES.CommandRegistration).toDynamicValue((ctx) => ({
                kind: actionType,
                factory: (action: Action) => {
                    const childContainer = new Container();
                    childContainer.parent = ctx.container;
                    childContainer.bind(TYPES.Action).toConstantValue(action);
                    return childContainer.get<ICommand>(InitializeViewportLayoutCommand);
                }
            }));
        }
    },
    { featureId: Symbol("viewport") }
);
