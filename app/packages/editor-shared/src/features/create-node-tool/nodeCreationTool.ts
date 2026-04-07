import type { Action, GModelElement, Disposable, GhostElement } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { DisposableCollection } = sharedImport("@eclipse-glsp/sprotty");
const {
    NodeCreationTool: GLSPNodeCreationTool,
    NodeCreationToolMouseListener: GLSPNodeCreationToolMouseListener
} = sharedImport("@eclipse-glsp/client");

/**
 * Mouse listener subclass that treats a drag-release as equivalent to a plain click-release.
 *
 * The upstream {@link NodeCreationToolMouseListener} only overrides {@link nonDraggingMouseUp},
 * so any mouse movement while the button is held — even the slightest cursor wobble — suppresses
 * node creation because {@link DragAwareMouseListener} promotes the event to {@link draggingMouseUp}.
 * This subclass delegates {@link draggingMouseUp} back to {@link nonDraggingMouseUp} so the node
 * is always created on mouse-up regardless of incidental drag.
 */
class DragTolerantNodeCreationMouseListener extends GLSPNodeCreationToolMouseListener {
    /**
     * Treats a drag-release identically to a plain click-release, so minor cursor movement
     * during the mouse-down / mouse-up cycle does not suppress node creation.
     *
     * @param ctx   The model element under the cursor.
     * @param event The native mouse-up event.
     * @returns The same actions as {@link nonDraggingMouseUp}.
     */
    override draggingMouseUp(ctx: GModelElement, event: MouseEvent): Action[] {
        return this.nonDraggingMouseUp(ctx, event);
    }
}

/**
 * Custom {@link NodeCreationTool} override that swaps the upstream mouse listener
 * with {@link DragTolerantNodeCreationMouseListener}.
 *
 * This ensures that node creation is triggered on every mouse-up event regardless
 * of whether the cursor moved slightly between mouse-down and mouse-up.
 */
@injectable()
export class NodeCreationTool extends GLSPNodeCreationTool {
    protected override createNodeCreationListener(ghostElement: GhostElement): Disposable {
        const toolListener = new DragTolerantNodeCreationMouseListener(this.triggerAction, this, ghostElement);
        return new DisposableCollection(toolListener, this.mouseTool.registerListener(toolListener));
    }
}
