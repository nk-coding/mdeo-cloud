import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeViewBase, type GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering pattern link end nodes (source or target).
 * Renders pure SVG since it is positioned inside an edge's SVG context.
 * Selection and resize handles are provided by the base GNodeView.
 */
@injectable()
export class GPatternLinkEndNodeView extends GNodeViewBase {
    /**
     * Renders the pattern link end node as an SVG group containing its children.
     * Selection and resize handles are provided by the base GNodeView.
     *
     * @param model The node model
     * @param context The rendering context
     * @returns The rendered VNode
     */
    override render(model: Readonly<GNode>, context: RenderingContext): VNode | undefined {
        const children = context.renderChildren(model);

        return svg(
            "g",
            {
                class: { "cursor-pointer": true },
                attrs: { [ATTR_BBOX_ELEMENT]: true }
            },
            ...this.renderControlElements(model),
            ...children
        );
    }
}
