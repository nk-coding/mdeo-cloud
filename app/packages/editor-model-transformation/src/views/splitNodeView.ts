import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeViewBase, type GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering split nodes.
 * Renders a diamond shape used for if/while branching (activity diagram split).
 */
@injectable()
export class GSplitNodeView extends GNodeViewBase {
    /**
     * The width of the split node
     */
    static readonly WIDTH = 80;

    /**
     * The height of the split node
     */
    static readonly HEIGHT = 50;

    /**
     * Renders the split node as a diamond shape for if/while branching.
     * Selection and resize handles are provided by the base GNodeViewBase.
     *
     * @param model The node model
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    override render(model: Readonly<GNode>, _context: RenderingContext): VNode | undefined {
        const width = GSplitNodeView.WIDTH;
        const height = GSplitNodeView.HEIGHT;
        const halfWidth = width / 2;
        const halfHeight = height / 2;

        // Diamond points: top, right, bottom, left
        const diamondPoints = `${halfWidth},0 ${width},${halfHeight} ${halfWidth},${height} 0,${halfHeight}`;

        const diamond = svg("polygon", {
            class: {
                "fill-background": true,
                "stroke-foreground": true,
                "stroke-[1.5px]": true,
                "cursor-pointer": true
            },
            attrs: {
                points: diamondPoints
            }
        });

        return svg("g", { class: { "cursor-pointer": true } }, ...this.renderControlElements(model), diamond);
    }
}
