import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode, VNodeStyle } from "snabbdom";
import { sharedImport } from "../sharedImport.js";
import type { GNode } from "../model/node.js";
import { GNodeView } from "./nodeView.js";

const { injectable } = sharedImport("inversify");
const { html, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Abstract view for rendering simple node elements without borders.
 * Provides a basic container with cursor classes and flex layout.
 */
@injectable()
export abstract class GSimpleNodeView extends GNodeView {
    protected override renderForeignElement(
        model: Readonly<GNode>,
        context: RenderingContext,
        children: readonly GModelElement[]
    ): VNode {
        const style: VNodeStyle = {};
        if (model.meta?.prefWidth != undefined) {
            style["max-width"] = `${model.meta.prefWidth}px`;
        }
        return html(
            "div",
            {
                class: {
                    ...this.getClasses(model)
                },
                attrs: {
                    [ATTR_BBOX_ELEMENT]: true
                },
                style
            },
            ...this.renderNodeContent(model, context, children)
        );
    }

    /**
     * Returns the CSS classes to be applied to the simple node's main visual element.
     * Subclasses can override this method to customize or extend the default classes.
     *
     * @param _model - The node model being rendered
     * @returns An object mapping class names to boolean values
     */
    protected getClasses(_model: Readonly<GNode>): Record<string, boolean> {
        return {
            flex: true,
            "flex-col": true,
            "cursor-pointer": true,
            "w-fit": true
        };
    }

    /**
     * Renders the content inside the simple node.
     * Default implementation renders all children.
     *
     * @param model The node model being rendered
     * @param context The rendering context
     * @returns An array of VNodes representing the content of the node
     */
    protected renderNodeContent(
        model: Readonly<GNode>,
        context: RenderingContext,
        children: readonly GModelElement[]
    ): VNode[] {
        return children.map((child) => context.renderElement(child)).filter((v): v is VNode => v !== undefined);
    }
}
