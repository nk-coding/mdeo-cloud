import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeViewBase, type GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering start nodes.
 * Renders a filled black circle representing the start of a control flow, similar to UML activity diagrams.
 * Selection and resize handles are provided by the base GNodeView.
 */
@injectable()
export class GStartNodeView extends GNodeViewBase {
    /**
     * The radius of the start node circle
     */
    static readonly RADIUS = 12;

    /**
     * Renders the start node as a filled black circle.
     * Selection and resize handles are provided by the base GNodeView.
     *
     * @param model The node model
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    override render(model: Readonly<GNode>, _context: RenderingContext): VNode | undefined {
        const radius = GStartNodeView.RADIUS;

        const circle = svg("circle", {
            class: {
                "fill-foreground": true,
                "stroke-foreground": true,
                "cursor-pointer": true
            },
            attrs: {
                cx: radius,
                cy: radius,
                r: radius
            }
        });

        return svg("g", { class: { "cursor-pointer": true } }, ...this.renderControlElements(model), circle);
    }
}
