import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeViewBase, type GNode } from "@mdeo/editor-shared";
import type { GSplitNode } from "../model/splitNode.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering split nodes.
 * Renders a diamond shape used for if/while branching (activity diagram split).
 */
@injectable()
export class GSplitNodeView extends GNodeViewBase {
    /**
     * The width of the diamond
     */
    static readonly WIDTH = 80;

    /**
     * The height of the diamond
     */
    static readonly HEIGHT = 50;

    /**
     * Renders the diamond node with expression text inside.
     * Selection and resize handles are provided by the base GNodeViewBase.
     *
     * @param model The node model
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    override render(model: Readonly<GNode>, _context: RenderingContext): VNode | undefined {
        const splitModel = model as GSplitNode;
        void splitModel;
        const width = GSplitNodeView.WIDTH;
        const height = GSplitNodeView.HEIGHT;
        const halfWidth = width / 2;
        const halfHeight = height / 2;

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
