import type { Action, IActionHandler, ICommand } from "@eclipse-glsp/sprotty";
import type { GModelElement } from "@eclipse-glsp/sprotty";
import type { EnableToolsAction as EnableToolsActionType } from "@eclipse-glsp/client";
import { sharedImport } from "../../sharedImport.js";
import { HandTool } from "../hand-tool/handTool.js";

const { injectable } = sharedImport("inversify");
const { RankedSelectMouseListener, EnableToolsAction, EnableDefaultToolsAction } = sharedImport("@eclipse-glsp/client");

/**
 * Select mouse listener that suppresses all selection actions when the hand tool is active.
 * Replaces RankedSelectMouseListener via rebind in the hand-tool featureModule.
 */
@injectable()
export class HandAwareSelectMouseListener extends RankedSelectMouseListener implements IActionHandler {
    private preventSelection = false;

    handle(action: Action): void | Action | ICommand {
        if (action.kind === EnableToolsAction.KIND) {
            this.preventSelection = (action as EnableToolsActionType).toolIds.includes(HandTool.ID);
        } else if (action.kind === EnableDefaultToolsAction.KIND) {
            this.preventSelection = false;
        }
    }

    override mouseDown(target: GModelElement, event: MouseEvent): (Action | Promise<Action>)[] {
        if (this.preventSelection) {
            return [];
        }
        return super.mouseDown(target, event);
    }
}
