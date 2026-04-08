import type { Action, GModelElement } from "@eclipse-glsp/sprotty";
import { RemoteRedoAction, RemoteUndoAction } from "@mdeo/protocol-common";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { KeyListener } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Returns {@code true} when the event was produced with the platform's primary modifier key
 * (Ctrl on Windows/Linux, Cmd/Meta on macOS).
 *
 * @param event The keyboard event to inspect.
 * @returns {@code true} if Ctrl or Meta is held.
 */
function isCtrlOrCmd(event: KeyboardEvent): boolean {
    return event.ctrlKey || event.metaKey;
}

/**
 * GLSP key listener that intercepts undo/redo keyboard shortcuts in the graphical editor
 * and dispatches {@link RemoteUndoAction} / {@link RemoteRedoAction}.
 *
 * <p>Registered as {@code TYPES.KeyListener} via {@link remoteUndoRedoModule}.
 */
@injectable()
export class RemoteUndoRedoKeyListener extends KeyListener {
    /**
     * Intercepts keyboard events and converts undo/redo shortcuts to the corresponding
     * remote actions.
     *
     * @param _element The focused graphical model element (unused).
     * @param event The keyboard event.
     * @returns A {@link RemoteUndoAction} or {@link RemoteRedoAction} when matched, otherwise empty.
     */
    override keyDown(_element: GModelElement, event: KeyboardEvent): Action[] {
        if (!isCtrlOrCmd(event)) {
            return [];
        }

        const key = event.key.toLowerCase();

        if (key === "y" || (key === "z" && event.shiftKey)) {
            return [RemoteRedoAction.create()];
        }

        if (key === "z" && !event.shiftKey) {
            return [RemoteUndoAction.create()];
        }

        return [];
    }
}
