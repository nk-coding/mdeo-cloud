import { sharedImport } from "../../sharedImport.js";
import { RevealSourceMouseListener } from "./revealSourceMouseListener.js";

const { FeatureModule, TYPES } = sharedImport("@eclipse-glsp/client");

/**
 * GLSP client feature module that registers the {@link RevealSourceMouseListener} singleton.
 *
 * Once loaded, alt+clicking any graphical element or double-clicking an issue-marker
 * badge will dispatch a {@link RevealSourceAction} to the diagram server, which in turn
 * sends a {@code textDocument/revealSource} LSP notification to the workbench so the
 * corresponding source range is selected in the Monaco textual editor.
 */
export const revealSourceModule = new FeatureModule(
    (bind) => {
        bind(RevealSourceMouseListener).toSelf().inSingletonScope();
        bind(TYPES.MouseListener).toService(RevealSourceMouseListener);
    },
    { featureId: Symbol("revealSource") }
);
