import type { GModelElement } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { FeedbackMoveMouseListener: GLSPFeedbackMoveMouseListener } = sharedImport("@eclipse-glsp/client");

/**
 * Custom feedback move mouse listener that prevents move initialization when interacting with resize handles.
 * This ensures that resize handles don't trigger move operations.
 */
export class FeedbackMoveMouseListener extends GLSPFeedbackMoveMouseListener {
    /**
     * Initializes a move operation, but skips initialization if the event target is a resize handle
     * or if the alt key is held (which means the user intends a reveal-source action, not a move).
     *
     * @param target The target element to move
     * @param event The mouse event that triggered the move
     */
    protected override initializeMove(target: GModelElement, event: MouseEvent): void {
        if (event.altKey) {
            return;
        }
        const eventTarget = event?.target;
        if (eventTarget instanceof SVGElement) {
            const location = eventTarget.dataset["resizeHandleLocation"];
            if (typeof location === "string") {
                return;
            }
        }
        super.initializeMove(target, event);
    }
}
