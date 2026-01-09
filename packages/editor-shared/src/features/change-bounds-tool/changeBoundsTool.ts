import type {
    ISelectionListener,
    ResizeHandleLocation as ResizeHandleLocationType,
    GResizeHandle as GResizeHandleType,
    SelectableBoundsAware
} from "@eclipse-glsp/client";
import type { Action, GModelElement, MouseListener, Operation } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import { PartialChangeBoundsOperation, type PartialElementAndBounds } from "@mdeo/editor-protocol";

const { injectable } = sharedImport("inversify");
const {
    ChangeBoundsTool: GLSPChangeBoundsTool,
    ChangeBoundsListener: GLSPChangeBoundsListener,
    GResizeHandle,
    FeedbackMoveMouseListener: GLSPFeedbackMoveMouseListener,
    Point,
    Dimension,
    ResizeHandleLocation
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
                const resizeHandle = new GResizeHandle(location as ResizeHandleLocationType);
                // @ts-expect-error not readonly
                resizeHandle.parent = target;
                actualTarget = resizeHandle;
            }
        }
        return super.updateResizeElement(actualTarget, event);
    }

    protected override handleResizeOnServer(activeResizeHandle: GResizeHandleType): Action[] {
        const resizedElement = activeResizeHandle.parent;
        if (this.initialBounds && this.isValidResize(resizedElement)) {
            const location = activeResizeHandle.location;
            const elementAndBounds: PartialElementAndBounds = {
                elementId: resizedElement.id,
                newPosition: {
                    x: resizedElement.bounds.x,
                    y: resizedElement.bounds.y
                },
                newSize: {
                    width:
                        location !== ResizeHandleLocation.Top && location !== ResizeHandleLocation.Bottom
                            ? resizedElement.bounds.width
                            : undefined,
                    height:
                        location !== ResizeHandleLocation.Left && location !== ResizeHandleLocation.Right
                            ? resizedElement.bounds.height
                            : undefined
                }
            };
            if (!this.initialBounds.newPosition || !elementAndBounds.newPosition) {
                return [];
            }
            if (
                !Point.equals(this.initialBounds.newPosition, elementAndBounds.newPosition) ||
                !Dimension.equals(this.initialBounds.newSize, resizedElement.bounds)
            ) {
                this.initialBounds = undefined;
                return [PartialChangeBoundsOperation.create([elementAndBounds])];
            }
        }
        return [];
    }

    protected override handleMoveElementsOnServer(elementsToMove: SelectableBoundsAware[]): Operation[] {
        const newBounds = elementsToMove.map((element) => ({
            elementId: element.id,
            newPosition: {
                x: element.bounds.x,
                y: element.bounds.y
            }
        }));
        return newBounds.length > 0 ? [PartialChangeBoundsOperation.create(newBounds)] : [];
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
