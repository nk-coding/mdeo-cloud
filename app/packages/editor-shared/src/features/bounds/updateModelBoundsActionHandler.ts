import type { Action, IActionHandler, ICommand } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { UpdateModelAction, SetModelAction } = sharedImport("@eclipse-glsp/sprotty");
const { LocalRequestBoundsAction } = sharedImport("@eclipse-glsp/client");

/**
 * Action handler that triggers bounds update on update/set model actions
 */
@injectable()
export class UpdateModelBoundsActionHandler implements IActionHandler {
    handle(action: Action): ICommand | Action | void {
        if (UpdateModelAction.is(action) || SetModelAction.is(action)) {
            return LocalRequestBoundsAction.create(action.newRoot);
        }
    }
}
