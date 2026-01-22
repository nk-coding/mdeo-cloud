import type { Action } from "@eclipse-glsp/protocol";

/**
 * Action to reset the bounds of the root canvas.
 * This is typically triggered when the canvas container is resized.
 */
export interface ResetCanvasBoundsAction extends Action {
    kind: "resetCanvasBoundsAction";
}
