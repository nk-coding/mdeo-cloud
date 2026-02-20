import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { GNode } from "../model/node.js";
import type { VNode } from "snabbdom";
import { GNodeViewBase } from "./nodeViewBase.js";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Abstract view for node elements that render their content inside an HTML foreignObject.
 * Extends {@link GNodeViewBase} with a fixed render() that wraps the result of
 * renderForeignElement() in a full-viewport foreignObject alongside the control elements.
 *
 * Subclasses only need to implement renderForeignElement().
 * For nodes that render pure SVG, extend {@link GNodeViewBase} directly instead.
 */
@injectable()
export abstract class GNodeView extends GNodeViewBase {
    override render(model: Readonly<GNode>, context: RenderingContext): VNode | undefined {
        const foreignObjectVNode = svg(
            "foreignObject",
            {
                class: {
                    "pointer-events-none": true,
                    "[&_*]:pointer-events-auto": true
                },
                attrs: {
                    x: "0",
                    y: "0",
                    width: "99999",
                    height: "99999",
                    [ATTR_BBOX_ELEMENT]: true
                }
            },
            this.renderForeignElement(model, context)
        );
        return svg("g", null, ...this.renderControlElements(model), foreignObjectVNode);
    }

    /**
     * Renders the content inside the foreignObject element.
     *
     * @param model The HTML node model
     * @param context The rendering context
     * @returns The VNode representing the foreign element's content
     */
    protected abstract renderForeignElement(model: Readonly<GNode>, context: RenderingContext): VNode;
}
