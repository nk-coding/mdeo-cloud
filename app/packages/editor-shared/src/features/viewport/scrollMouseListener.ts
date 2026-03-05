import type { Action, ICommand } from "@eclipse-glsp/sprotty";
import type { GModelElement } from "@eclipse-glsp/sprotty";
import type { EnableToolsAction as EnableToolsActionType } from "@eclipse-glsp/client";
import { sharedImport } from "../../sharedImport.js";
import { HandTool } from "../hand-tool/handTool.js";

const { injectable } = sharedImport("inversify");
const { GLSPScrollMouseListener, EnableToolsAction, EnableDefaultToolsAction, MarqueeMouseTool } =
    sharedImport("@eclipse-glsp/client");
const { findParentByFeature, isViewport } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Extended scroll mouse listener that adds support for the hand tool's
 * "scroll always" mode, which pans the viewport even when the pointer is over
 * a moveable element (bypassing the default check that prevents panning over nodes/edges).
 *
 * Replaces GLSPScrollMouseListener via rebind in the viewport featureModule.
 */
@injectable()
export class EditorScrollMouseListener extends GLSPScrollMouseListener {
    /**
     * When true, pans the viewport on drag regardless of what element was hit.
     */
    scrollAlways = false;

    override handle(action: Action): void | Action | ICommand {
        if (action.kind === EnableToolsAction.KIND) {
            const toolIds = (action as EnableToolsActionType).toolIds;
            if (toolIds.includes(MarqueeMouseTool.ID)) {
                this.preventScrolling = true;
                this.scrollAlways = false;
            } else if (toolIds.includes(HandTool.ID)) {
                this.scrollAlways = true;
                this.preventScrolling = false;
            } else {
                this.preventScrolling = false;
                this.scrollAlways = false;
            }
        } else if (action.kind === EnableDefaultToolsAction.KIND) {
            this.preventScrolling = false;
            this.scrollAlways = false;
        }
    }

    override mouseDown(target: GModelElement, event: MouseEvent): (Action | Promise<Action>)[] {
        if (this.preventScrolling) {
            return [];
        }
        if (this.scrollAlways) {
            const viewport = findParentByFeature(target, isViewport);
            if (viewport) {
                this.lastScrollPosition = { x: event.pageX, y: event.pageY };
            } else {
                this.lastScrollPosition = undefined;
                this.scrollbar = undefined;
            }
            return [];
        }
        return super.mouseDown(target, event);
    }
}
