import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GHtmlNode } from "../model/htmlNode.js";
import { GNodeView } from "./nodeView.js";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Abstract view for rendering HTML node elements within an SVG context.
 */
@injectable()
export abstract class GHtmlNodeView extends GNodeView {
    override render(model: Readonly<GHtmlNode>, context: RenderingContext): VNode | undefined {
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
     * Abstract method to render the content inside the foreign element
     *
     * @param model The HTML node model
     * @param context The rendering context
     * @returns The VNode representing the foreign element's content
     */
    protected abstract renderForeignElement(model: Readonly<GHtmlNode>, context: RenderingContext): VNode;
}
