import type { Action } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";

const { FitToScreenAction } = sharedImport("@eclipse-glsp/protocol");

/**
 * Creates a new action to fit the viewport to the current diagram with common params.
 *
 * @param animate whether to animate the action
 */
export function createFitToScreenAction(animate: boolean | undefined = undefined, elementIds: string[]): Action {
    return FitToScreenAction.create(elementIds, { padding: 50, maxZoom: 2, animate });
}
