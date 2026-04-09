import type { Action } from "@eclipse-glsp/protocol";

/**
 * GLSP action dispatched from the graphical editor client to request an **undo** in the
 * associated textual editor.
 *
 * <p>This action is handled entirely on the client side (it never reaches the diagram server).
 * A client-side handler intercepts it and delegates to the Monaco editor's built-in undo
 * command, effectively making Ctrl+Z / Cmd+Z in the graphical view undo the last text change.
 */
export interface RemoteUndoAction extends Action {
    kind: typeof RemoteUndoAction.KIND;
}

/**
 * Namespace for {@link RemoteUndoAction}.
 */
export namespace RemoteUndoAction {
    /**
     * Discriminant string for {@link RemoteUndoAction}.
     */
    export const KIND = "remoteUndo" as const;

    /**
     * Creates a new {@link RemoteUndoAction}.
     *
     * @returns A new {@link RemoteUndoAction}.
     */
    export function create(): RemoteUndoAction {
        return { kind: KIND };
    }
}

/**
 * GLSP action dispatched from the graphical editor client to request a **redo** in the
 * associated textual editor.
 *
 * <p>This action is handled entirely on the client side (it never reaches the diagram server).
 * A client-side handler intercepts it and delegates to the Monaco editor's built-in redo
 * command, effectively making Ctrl+Y / Ctrl+Shift+Z / Cmd+Shift+Z in the graphical view
 * redo the last undone text change.
 */
export interface RemoteRedoAction extends Action {
    kind: typeof RemoteRedoAction.KIND;
}

/**
 * Namespace for {@link RemoteRedoAction}.
 */
export namespace RemoteRedoAction {
    /**
     * Discriminant string for {@link RemoteRedoAction}.
     */
    export const KIND = "remoteRedo" as const;

    /**
     * Creates a new {@link RemoteRedoAction}.
     *
     * @returns A new {@link RemoteRedoAction}.
     */
    export function create(): RemoteRedoAction {
        return { kind: KIND };
    }
}
