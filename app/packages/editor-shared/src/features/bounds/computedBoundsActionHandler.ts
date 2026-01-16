import type { Action, IActionHandler, ICommand } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { injectable} = sharedImport("inversify");
const { ComputedBoundsAction } = sharedImport("@eclipse-glsp/sprotty");
const { LocalComputedBoundsAction } = sharedImport("@eclipse-glsp/client");

/**
 * Action handler that submits a local computed bounds action for each non-local computed bounds action.
 * Ensures that layout data is ALWAYS set on the client side
 */
@injectable()
export class ComputedBoundsActionHandler implements IActionHandler {

    handle(action: Action): ICommand | Action | void {
        if (ComputedBoundsAction.is(action) && !LocalComputedBoundsAction.is(action)) {
            const newAction = {...action };
            LocalComputedBoundsAction.mark(newAction);
            return newAction;
        }
    }
    
}