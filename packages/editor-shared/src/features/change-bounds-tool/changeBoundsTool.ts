import type { ISelectionListener, ResizeHandleLocation, TrackedResize } from "@eclipse-glsp/client";
import type { GModelElement, MouseListener } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const {
    ChangeBoundsTool: GLSPChangeBoundsTool,
    ChangeBoundsListener: GLSPChangeBoundsListener,
    GResizeHandle,
    FeedbackMoveMouseListener: GLSPFeedbackMoveMouseListener
} = sharedImport("@eclipse-glsp/client");

/**
 * Custom change bounds tool that supports custom resize handles and move behavior.
 * Overrides the default GLSP change bounds tool to work with custom SVG-based resize handles.
 */
@injectable()
export class ChangeBoundsTool extends GLSPChangeBoundsTool {
    /**
     * Creates a custom change bounds listener that handles resize operations.
     * 
     * @returns The change bounds listener
     */
    protected override createChangeBoundsListener(): MouseListener & ISelectionListener {
        return new ChangeBoundsListener(this);
    }

    /**
     * Creates a custom move mouse listener that handles move operations.
     * 
     * @returns The move mouse listener
     */
    protected override createMoveMouseListener(): MouseListener {
        return new FeedbackMoveMouseListener(this);
    }
}

/**
 * Custom change bounds listener that handles resize operations with custom SVG-based resize handles.
 * Interprets resize handle locations from SVG data attributes to support invisible resize handles.
 */
export class ChangeBoundsListener extends GLSPChangeBoundsListener {
    /**
     * Updates the resize element by extracting resize handle location from SVG data attributes.
     * Creates a virtual GResizeHandle when a resize handle location is detected in the event target.
     * 
     * @param target The target element to resize
     * @param event The mouse event that triggered the resize
     * @returns True if the resize element was updated successfully
     */
    protected override updateResizeElement(target: GModelElement, event?: MouseEvent): boolean {
        let actualTarget = target;
        const eventTarget = event?.target;
        if (eventTarget instanceof SVGElement) {
            const location = eventTarget.dataset["resizeHandleLocation"];
            if (typeof location === "string") {
                const resizeHandle = new GResizeHandle(location as ResizeHandleLocation);
                // @ts-expect-error not readonly
                resizeHandle.parent = target;
                actualTarget = resizeHandle;
            }
        }
        return super.updateResizeElement(actualTarget, event);
    }

    /**
     * Adds resize feedback to the diagram during a resize operation.
     * 
     * @param resize The tracked resize information
     * @param target The target element being resized
     * @param event The mouse event
     */
    protected override addResizeFeedback(resize: TrackedResize, target: GModelElement, event: MouseEvent): void {
        super.addResizeFeedback(resize, target, event);
    }
}

/**
 * Custom feedback move mouse listener that prevents move initialization when interacting with resize handles.
 * This ensures that resize handles don't trigger move operations.
 */
export class FeedbackMoveMouseListener extends GLSPFeedbackMoveMouseListener {

    /**
     * Initializes a move operation, but skips initialization if the event target is a resize handle.
     * This prevents conflicts between move and resize operations.
     * 
     * @param target The target element to move
     * @param event The mouse event that triggered the move
     */
    protected override initializeMove(target: GModelElement, event: MouseEvent): void {
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
