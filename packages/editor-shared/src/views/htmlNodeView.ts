import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GHtmlNode } from "../model/htmlNode.js";
import { GNodeView } from "./nodeView.js";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export abstract class GHtmlNodeView extends GNodeView {
    override render(model: Readonly<GHtmlNode>, context: RenderingContext, args?: {}): VNode | undefined {
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
            this.renderForeignElement(model, context, args)
        );

        return svg("g", {}, foreignObjectVNode, ...this.renderControlElements(model));
    }

    /**
     * Abstract method to render the content inside the foreign element
     */
    protected abstract renderForeignElement(model: Readonly<GHtmlNode>, context: RenderingContext, args?: {}): VNode;
}
