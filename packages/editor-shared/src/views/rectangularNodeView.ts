import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "../sharedImport.js";
import { GHtmlNodeView } from "./htmlNodeView.js";
import type { GRectangularNode } from "../model/rectangularNode.js";

const { injectable } = sharedImport("inversify");
const { html, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export abstract class GRectangularNodeView extends GHtmlNodeView {
    protected override renderForeignElement(
        model: Readonly<GRectangularNode>,
        context: RenderingContext,
        args?: {}
    ): VNode {
        return html(
            "div",
            {
                class: {
                    ...this.getClasses(model)
                },
                attrs: {
                    [ATTR_BBOX_ELEMENT]: true
                }
            },
            ...this.renderNodeContent(model, context, args)
        );
    }

    /**
     * Returns the CSS classes to be applied to the rectangular node's main visual element.
     * Subclasses can override this method to customize or extend the default classes.
     *
     * @param _model - The rectangular node model being rendered
     * @returns An object mapping class names to boolean values
     */
    protected getClasses(_model: Readonly<GRectangularNode>): Record<string, boolean> {
        return {
            "border-2": true,
            "border-foreground": true,
            "bg-background": true,
            "box-border": true,
            flex: true,
            "flex-col": true,
            "w-fit": true,
            "cursor-pointer": true
        };
    }

    /**
     * Abstract method to render the content inside the rectangular node
     */
    protected abstract renderNodeContent(
        model: Readonly<GRectangularNode>,
        context: RenderingContext,
        args?: {}
    ): VNode[];
}
