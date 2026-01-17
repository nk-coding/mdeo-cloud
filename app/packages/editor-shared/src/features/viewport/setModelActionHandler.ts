import type { Action } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import type { IActionHandler, ICommand } from "@eclipse-glsp/sprotty";
import { createFitToScreenAction } from "./fitToScreenAction.js";

const { injectable } = sharedImport("inversify");

/**
 * Action handler that executes fit-to-screen whenever the model has been set (initial page load)
 */
@injectable()
export class SetModelActionHandler implements IActionHandler {
    handle(): ICommand | Action | void {
        return createFitToScreenAction(false);
    }
}
