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
        if (cause === undefined || cause.kind !== RequestBoundsAction.KIND) {
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
                    },
                    newPosition: {
                        x: boundsData.bounds.x,
                        y: boundsData.bounds.y
                    }
                };
                resizes.push(resize);
            }
        });

        const responseId = (cause as RequestBoundsActionType).requestId;
        const revision = this.root !== undefined ? this.root.revision : undefined;
        const computedBoundsAction = ComputedBoundsAction.create(resizes, { revision, responseId });
        if (LocalRequestBoundsAction.is(cause)) {
            LocalComputedBoundsAction.mark(computedBoundsAction);
        }
        this.actionDispatcher.dispatch(computedBoundsAction);

        this.getElement2BoundsData().clear();
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
        if (!isSVGGraphicsElement(elm) || elm.nodeName !== "foreignObject") {
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
