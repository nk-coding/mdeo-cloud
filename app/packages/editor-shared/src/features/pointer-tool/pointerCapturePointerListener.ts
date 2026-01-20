import type { Action } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import { PointerListener } from "./pointerTool.js";
import type { GModelElement } from "@eclipse-glsp/sprotty";

const { injectable } = sharedImport("inversify");

/**
 * Pointer listener that captures the pointer when a pointer down event occurs.
 * This ensures that all subsequent pointer events are received by the same element,
 * even if the pointer moves outside its boundaries.
 */
@injectable()
export class PointerCapturePointerListener extends PointerListener {
    /**
     * Whether the pointer is currently captured.
     */
    private hasPointerCapture = false;

    override pointerDown(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[] {
        this.capturePointer(event);
        return [];
    }

    override pointerMove(target: GModelElement, event: PointerEvent): (Action | Promise<Action>)[] {
        if (event.buttons > 0 && !this.hasPointerCapture) {
            this.capturePointer(event);
        }
        return [];
    }

    override lostPointerCapture(_target: GModelElement, _event: PointerEvent): (Action | Promise<Action>)[] {
        this.hasPointerCapture = false;
        return [];
    }

    /**
     * Captures the pointer for the event target.
     *
     * @param event The pointer event
     */
    protected capturePointer(event: PointerEvent): void {
        const htmlElement = event.target as HTMLElement | undefined;
        htmlElement?.setPointerCapture(event.pointerId);
    }
}
