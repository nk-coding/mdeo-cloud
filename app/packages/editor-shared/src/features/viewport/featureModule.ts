import { sharedImport } from "../../sharedImport.js";
import { SetViewportCommand } from "./viewport.js";
import { ZoomMouseListener } from "./zoom.js";
import { InitializeViewportLayoutCommand } from "./initializeViewportLayout.js";
import { EditorScrollMouseListener } from "./scrollMouseListener.js";
import type { Action, ICommand } from "@eclipse-glsp/sprotty";
import { FitToScreenCommand } from "./fitToScreen.js";
import { MdeoHiddenModelViewer } from "./hiddenModelViewer.js";

const { FeatureModule, GLSPScrollMouseListener } = sharedImport("@eclipse-glsp/client");
const {
    ZoomMouseListener: SprottyZoomMouseListener,
    SetViewportCommand: SprottySetViewportCommand,
    FitToScreenCommand: SprottyFitToScreenCommand,
    HiddenModelViewer,
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
        bind(FitToScreenCommand).toSelf();
        rebind(SprottyFitToScreenCommand).toService(FitToScreenCommand);
        bind(EditorScrollMouseListener).toSelf().inSingletonScope();
        rebind(GLSPScrollMouseListener).toService(EditorScrollMouseListener);
        bind(MdeoHiddenModelViewer).toSelf().inSingletonScope();
        rebind(HiddenModelViewer).toService(MdeoHiddenModelViewer);

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
