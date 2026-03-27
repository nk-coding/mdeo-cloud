import { sharedImport } from "../../sharedImport.js";
import { IssueMarkerUIExtension } from "./issueMarkerUIExtension.js";
import { IssueMarkerMouseListener } from "./issueMarkerMouseListener.js";
import { NoopDecorationPlacer } from "./noopDecorationPlacer.js";
import { IssueMarkerHoverEnterAction, IssueMarkerHoverLeaveAction } from "./issueMarkerActions.js";
import { ApplyMarkersCommand, DeleteMarkersCommand } from "./markerCommands.js";
import { ISSUE_MARKER_UI_EXTENSION_TOKEN } from "./tokens.js";

const {
    FeatureModule,
    configureActionHandler,
    GlspDecorationPlacer,
    ApplyMarkersCommand: GlspApplyMarkersCommand,
    DeleteMarkersCommand: GlspDeleteMarkersCommand,
    TYPES
} = sharedImport("@eclipse-glsp/client");

/**
 * Feature module that:
 * - Replaces the default {@link GlspDecorationPlacer} with {@link NoopDecorationPlacer}.
 * - Rebinds {@link GlspApplyMarkersCommand} and {@link GlspDeleteMarkersCommand} to local
 *   subclasses that create {@link GIssueMarker} instances which do not extend GLSP's own
 *   `GIssueMarker`, bypassing the `instanceof` check that triggers the unsupported
 *   pre-rendered popup warning.
 * - Registers {@link IssueMarkerUIExtension} as a singleton wired as `IDiagramStartup`,
 *   `IVNodePostprocessor`, and `IActionHandler`.
 * - Registers {@link IssueMarkerMouseListener} as a `MouseListener` for badge hit-testing.
 */
export const decorationModule = new FeatureModule(
    (bind, _unbind, isBound, rebind) => {
        rebind(GlspDecorationPlacer).to(NoopDecorationPlacer).inSingletonScope();
        rebind(GlspApplyMarkersCommand).to(ApplyMarkersCommand);
        rebind(GlspDeleteMarkersCommand).to(DeleteMarkersCommand);

        bind(IssueMarkerUIExtension).toSelf().inSingletonScope();
        bind(TYPES.IDiagramStartup).toService(IssueMarkerUIExtension);
        bind(TYPES.IVNodePostprocessor).toService(IssueMarkerUIExtension);

        const context = { bind, isBound };
        configureActionHandler(context, IssueMarkerHoverEnterAction.KIND, IssueMarkerUIExtension);
        configureActionHandler(context, IssueMarkerHoverLeaveAction.KIND, IssueMarkerUIExtension);

        bind(ISSUE_MARKER_UI_EXTENSION_TOKEN).toService(IssueMarkerUIExtension);

        bind(IssueMarkerMouseListener).toSelf().inSingletonScope();
        bind(TYPES.MouseListener).toService(IssueMarkerMouseListener);
    },
    { featureId: Symbol("decoration") }
);
