import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "@mdeo/editor-shared";
import type { GPatternLinkEndNode } from "../model/patternLinkEndNode.js";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering pattern link end nodes (source or target).
 * Renders pure SVG since it's positioned inside an edge's SVG context.
 */
@injectable()
export class GPatternLinkEndNodeView implements IView {
    /**
     * Renders the pattern link end node as SVG.
     * Renders children (labels) inside an SVG group.
     *
     * @param model The pattern link end node model
     * @param context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GPatternLinkEndNode>, context: RenderingContext): VNode {
        const children = context.renderChildren(model);

        return svg(
            "g",
            {
                class: {
                    "cursor-pointer": true
                },
                attrs: {
                    [ATTR_BBOX_ELEMENT]: true
                }
            },
            ...children
        );
    }
}
