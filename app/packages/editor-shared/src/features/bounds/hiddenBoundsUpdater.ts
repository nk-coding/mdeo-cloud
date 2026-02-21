import type {
    Action,
    Bounds,
    BoundsAware,
    ElementAndBounds,
    GModelElement,
    RequestBoundsAction as RequestBoundsActionType
} from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { GLSPHiddenBoundsUpdater, LocalRequestBoundsAction, LocalComputedBoundsAction } =
    sharedImport("@eclipse-glsp/client");
const {
    isSVGGraphicsElement,
    Bounds: SprottyBounds,
    RequestBoundsAction,
    ComputedBoundsAction
} = sharedImport("@eclipse-glsp/sprotty");

/**
 * Custom hidden bounds updater that manages bounds computation for foreign objects
 */
@injectable()
export class HiddenBoundsUpdater extends GLSPHiddenBoundsUpdater {
    /**
     * Simplified vesion that computes neither layoutData, alignments, nor routes
     *
     * @param cause the action that caused the update
     */
    override postUpdate(cause?: Action): void {
        if (!this.shouldHandle(cause)) {
            return;
        }

        if (LocalRequestBoundsAction.is(cause) && cause.elementIDs) {
            this.focusOnElements(cause.elementIDs);
        }

        this.getBoundsFromDOM();
        this.layouter.layout(this.getElement2BoundsData());

        const resizes: ElementAndBounds[] = [];
        this.getElement2BoundsData().forEach((boundsData, element) => {
            if (boundsData.boundsChanged && boundsData.bounds != undefined) {
                const resize: ElementAndBounds = {
                    elementId: element.id,
                    newSize: {
                        width: boundsData.bounds.width,
                        height: boundsData.bounds.height
                    }
                };
                resizes.push(resize);
            }
        });

        this.submitResponse(cause, resizes);
        this.getElement2BoundsData().clear();
    }

    /**
     * Determines whether the bounds updater should handle the given cause action.
     *
     * @param cause the action that may have caused a bounds update
     * @returns true if the cause is defined and is a RequestBoundsAction, false otherwise
     */
    protected shouldHandle(cause: Action | undefined): cause is Action {
        return cause !== undefined && RequestBoundsAction.is(cause);
    }

    /**
     * Submits the computed bounds as a response to the cause action.
     *
     * @param cause the action that caused the bounds update, used to correlate the response
     * @param resizes the list of elements with their new bounds to be included in the response
     */
    protected submitResponse(cause: Action, resizes: ElementAndBounds[]): void {
        const responseId = (cause as RequestBoundsActionType).requestId;
        const revision = this.root !== undefined ? this.root.revision : undefined;
        const computedBoundsAction = ComputedBoundsAction.create(resizes, { revision, responseId });
        if (LocalRequestBoundsAction.is(cause)) {
            LocalComputedBoundsAction.mark(computedBoundsAction);
        }
        this.actionDispatcher.dispatch(computedBoundsAction);
    }

    /**
     * Gets the bounds of an element, with special handling for foreign objects.
     * For foreign objects, calculates bounds from the first child element to properly support HTML content.
     *
     * @param elm The DOM node to measure
     * @param element The model element
     * @returns The bounds of the element
     */
    public override getBounds(elm: Node, element: GModelElement & BoundsAware): Bounds {
        if (!isSVGGraphicsElement(elm)) {
            const bounds = (elm as Element).getBoundingClientRect();
            return {
                x: bounds.x,
                y: bounds.y,
                width: bounds.width,
                height: bounds.height
            };
        } else if (elm.nodeName !== "foreignObject") {
            return super.getBounds(elm, element);
        }
        const firstChild = (elm as SVGForeignObjectElement).firstElementChild;
        if (firstChild == null) {
            return SprottyBounds.EMPTY;
        }
        const bounds = firstChild.getBoundingClientRect();
        const foreignObjectBounds = (elm as SVGForeignObjectElement).getBBox();
        return {
            x: foreignObjectBounds.x,
            y: foreignObjectBounds.y,
            width: bounds.width,
            height: bounds.height
        };
    }
}
