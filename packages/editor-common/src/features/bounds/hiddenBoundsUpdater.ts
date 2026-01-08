import type { Bounds, IVNodePostprocessor } from "@eclipse-glsp/sprotty";
import type { SModelElementImpl, InternalBoundsAware } from "sprotty";
import type { PluginContext } from "../../plugin/pluginContext.js";
import { findViewportZoom } from "../../base/findViewportZoom.js";

/**
 * Creates a custom hidden bounds updater that manages bounds computation for foreign objects
 */
export function createHiddenBoundsUpdater(context: PluginContext): {
    new(): IVNodePostprocessor
} {
    const { "@eclipse-glsp/client": glspClient, "@eclipse-glsp/sprotty": glspSprotty, inversify } = context;
    const { injectable } = inversify;
    const { GLSPHiddenBoundsUpdater } = glspClient;
    const { isSVGGraphicsElement, Bounds: SprottyBounds, isViewport } = glspSprotty;

    @injectable()
    class HiddenBoundsUpdaterImpl extends GLSPHiddenBoundsUpdater {

        protected override getBounds(elm: Node, element: SModelElementImpl & InternalBoundsAware): Bounds {
            if (!isSVGGraphicsElement(elm) || elm.nodeName !== "foreignObject") {
                return super.getBounds(elm, element);
            }
            const firstChild = elm.firstElementChild;
            if (firstChild == null) {
                return SprottyBounds.EMPTY;
            }
            const bounds = firstChild.getBoundingClientRect();
            const zoom = findViewportZoom(element, isViewport);
            return {
                x: bounds.x,
                y: bounds.y,
                width: bounds.width / zoom,
                height: bounds.height / zoom
            };
        }

    }

    return HiddenBoundsUpdaterImpl;
}
