import type { Bounds, IVNodePostprocessor } from "@eclipse-glsp/sprotty";
import type { SModelElementImpl, InternalBoundsAware } from "sprotty";
import { findViewportZoom } from "../../base/findViewportZoom.js";
import { sharedImport } from "../../sharedImport.js";

const { "@eclipse-glsp/client": glspClient, "@eclipse-glsp/sprotty": glspSprotty, inversify } = {
    "@eclipse-glsp/client": sharedImport("@eclipse-glsp/client"),
    "@eclipse-glsp/sprotty": sharedImport("@eclipse-glsp/sprotty"),
    inversify: sharedImport("inversify")
};
const { injectable } = inversify;
const { GLSPHiddenBoundsUpdater } = glspClient;
const { isSVGGraphicsElement, Bounds: SprottyBounds } = glspSprotty;

/**
 * Custom hidden bounds updater that manages bounds computation for foreign objects
 */
@injectable()
export class HiddenBoundsUpdater extends GLSPHiddenBoundsUpdater {

    /**
     * Gets the bounds of an element, with special handling for foreign objects.
     * For foreign objects, calculates bounds from the first child element to properly support HTML content.
     * 
     * @param elm The DOM node to measure
     * @param element The model element
     * @returns The bounds of the element
     */
    protected override getBounds(elm: Node, element: SModelElementImpl & InternalBoundsAware): Bounds {
        if (!isSVGGraphicsElement(elm) || elm.nodeName !== "foreignObject") {
            return super.getBounds(elm, element);
        }
        const firstChild = (elm as Element).firstElementChild;
        if (firstChild == null) {
            return SprottyBounds.EMPTY;
        }
        const bounds = firstChild.getBoundingClientRect();
        const zoom = findViewportZoom(element);
        return {
            x: bounds.x,
            y: bounds.y,
            width: bounds.width / zoom,
            height: bounds.height / zoom
        };
    }

}
