import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { GNode } from "../model/node.js";
import type { VNode } from "snabbdom";
import { GNodeViewBase } from "./nodeViewBase.js";

const { injectable } = sharedImport("inversify");
const { svg, html, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

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
    /**
     * Renders the node as a full-viewport `<foreignObject>` containing the result of
     * {@link renderForeignElement}, surrounded by background and foreground control elements
     * (selection rect, resize handles) and issue-marker badges.
     *
     * @param model The node model element to render.
     * @param context The current rendering context.
     * @returns An SVG `<g>` VNode wrapping all visual layers, or `undefined` if the
     *   element should not be rendered.
     */
    override render(model: Readonly<GNode>, context: RenderingContext): VNode | undefined {
        const { children, markers } = this.splitChildren(model);
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
            html(
                "div",
                {
                    class: {
                        relative: true,
                        "w-fit": true
                    }
                },
                this.renderForeignElement(model, context, children)
            )
        );
        return svg(
            "g",
            null,
            ...this.renderBackgroundControlElements(model),
            foreignObjectVNode,
            ...this.renderForegroundControlElements(model),
            ...this.renderIssueMarkers(markers, model, context)
        );
    }

    /**
     * Renders the content inside the foreignObject element.
     *
     * @param model The HTML node model
     * @param context The rendering context
     * @param children The child model elements to be rendered inside the foreignObject
     * @returns The VNode representing the foreign element's content
     */
    protected abstract renderForeignElement(
        model: Readonly<GNode>,
        context: RenderingContext,
        children: readonly GModelElement[]
    ): VNode;
}
