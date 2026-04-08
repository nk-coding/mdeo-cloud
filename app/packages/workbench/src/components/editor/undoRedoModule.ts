import { ContainerModule } from "inversify";
import type * as monacoType from "monaco-editor";
import { RemoteUndoAction, RemoteRedoAction } from "@mdeo/protocol-common";
import { onAction } from "@eclipse-glsp/client";

/**
 * Creates an inversify {@link ContainerModule} that intercepts
 * {@link RemoteUndoAction} and {@link RemoteRedoAction} on the GLSP client side
 * and delegates them to the Monaco editor's built-in undo/redo command.
 *
 * <p>Both actions are handled locally (never forwarded to the diagram server).
 * Monaco editors are located via {@code monaco.editor.getEditors()} and the target
 * editor is identified by matching its model URI against the provided {@code fileUri}.
 *
 * @param monaco The Monaco editor API (used to look up the active editors).
 * @param fileUri The URI of the file managed by this graphical editor, used to identify
 *   which Monaco editor should receive the undo/redo command.
 * @returns A container module that registers the undo/redo action handlers.
 */
export function createUndoRedoModule(monaco: typeof monacoType, fileUri: string): ContainerModule {
    return new ContainerModule((bind, _unbind, isBound) => {
        const context = { bind, isBound };

        onAction(context, RemoteUndoAction.KIND, () => {
            triggerUndoRedo(monaco, fileUri, "undo");
        });

        onAction(context, RemoteRedoAction.KIND, () => {
            triggerUndoRedo(monaco, fileUri, "redo");
        });
    });
}

/**
 * Finds the Monaco code editor whose model URI matches the given file URI and
 * triggers either the {@code undo} or {@code redo} editor action on it.
 *
 * @param monaco The Monaco editor API.
 * @param fileUri The URI of the file to undo/redo in.
 * @param command The editor action to trigger: {@code "undo"} or {@code "redo"}.
 */
function triggerUndoRedo(monaco: typeof monacoType, fileUri: string, command: "undo" | "redo"): void {
    const editors = monaco.editor.getEditors();
    const target = editors.find((editor) => editor.getModel()?.uri.toString() === fileUri);
    if (target != undefined) {
        target.focus();
        target.trigger("graphicalEditor", command, null);
    }
}
