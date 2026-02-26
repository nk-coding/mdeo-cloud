import type { Action } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import type { GModelRoot } from "@eclipse-glsp/sprotty";

const { FitToScreenAction } = sharedImport("@eclipse-glsp/protocol");
const { FitToScreenCommand: SprottyFitToScreenCommand } = sharedImport("@eclipse-glsp/sprotty");
const { injectable } = sharedImport("inversify");

/**
 * Creates a new action to fit the viewport to the current diagram with common params.
 *
 * @param animate whether to animate the action
 */
export function createFitToScreenAction(animate: boolean | undefined = undefined, elementIds: string[]): Action {
    return FitToScreenAction.create(elementIds, { padding: 50, maxZoom: 2, animate });
}

/**
 * Custom FitToScreenCommand that sets a fallback viewport if necessary
 */
@injectable()
export class FitToScreenCommand extends SprottyFitToScreenCommand {
    protected override initialize(model: GModelRoot): void {
        super.initialize(model);
        if (this.newViewport == undefined) {
            this.newViewport = {
                scroll: { x: 0, y: 0 },
                zoom: 1
            };
        }
    }
}
