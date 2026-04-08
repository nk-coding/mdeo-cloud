import { sharedImport } from "../../sharedImport.js";
import { RemoteUndoRedoKeyListener } from "./remoteUndoRedoKeyListener.js";

const { FeatureModule, TYPES } = sharedImport("@eclipse-glsp/client");

/**
 * GLSP client feature module that registers the {@link RemoteUndoRedoKeyListener} singleton.
 *
 * <p>Once loaded, pressing Ctrl+Z / Cmd+Z in the graphical editor dispatches a
 * {@link RemoteUndoAction}, and pressing Ctrl+Y / Ctrl+Shift+Z / Cmd+Shift+Z dispatches a
 * {@link RemoteRedoAction}.  Both actions are intercepted by a workbench-level container
 * module (see {@code undoRedoModule} in GraphicalEditor.vue) that delegates to Monaco's
 * native undo/redo command, keeping the graphical and textual editors in sync.
 */
export const remoteUndoRedoModule = new FeatureModule(
    (bind) => {
        bind(RemoteUndoRedoKeyListener).toSelf().inSingletonScope();
        bind(TYPES.KeyListener).toService(RemoteUndoRedoKeyListener);
    },
    { featureId: Symbol("remoteUndoRedo") }
);
