import type { Action, ElementAndBounds } from "@eclipse-glsp/sprotty";
import { HiddenBoundsUpdater, sharedImport } from "@mdeo/editor-shared";
import { IterativeRequestBoundsAction } from "./iterativeRequestBounds.js";

const { injectable } = sharedImport("inversify");
const { RequestBoundsAction } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Custom hidden bounds updater that does bounds computation using two rounds
 */
@injectable()
export class ModelTransformationHiddenBoundsUpdater extends HiddenBoundsUpdater {
    protected override shouldHandle(cause: Action | undefined): cause is Action {
        return (cause != undefined && IterativeRequestBoundsAction.is(cause)) || super.shouldHandle(cause);
    }

    protected override submitResponse(cause: Action, resizes: ElementAndBounds[]): void {
        if (IterativeRequestBoundsAction.is(cause)) {
            const newData = new Set(resizes.map((elementAndBounds) => elementAndBounds.elementId));
            const actualResizes = [
                ...cause.previousBounds.filter((elementAndBounds) => !newData.has(elementAndBounds.elementId)),
                ...resizes
            ];
            super.submitResponse(cause.cause, actualResizes);
        } else if (RequestBoundsAction.is(cause)) {
            this.actionDispatcher.dispatch(IterativeRequestBoundsAction.create(cause, resizes));
        }
    }
}
