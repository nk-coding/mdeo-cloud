import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "@mdeo/editor-shared";
import type { GMergeNode } from "../model/mergeNode.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering merge nodes.
 * Renders a small diamond shape where control flow branches merge back together.
 */
@injectable()
export class GMergeNodeView implements IView {
    /**
     * The size of the merge node diamond
     */
    static readonly SIZE = 20;

    /**
     * Renders the merge node as a small diamond.
     *
     * @param model The merge node model
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GMergeNode>, _context: RenderingContext): VNode {
        const size = GMergeNodeView.SIZE;
        const half = size / 2;

        // Diamond points: top, right, bottom, left
        const diamondPoints = `${half},0 ${size},${half} ${half},${size} 0,${half}`;

        const diamond = svg("polygon", {
            class: {
                "fill-foreground": true,
                "stroke-foreground": true,
                "cursor-pointer": true
            },
            attrs: {
                points: diamondPoints
            }
        });

        const rootClasses: Record<string, boolean> = {
            "cursor-pointer": true
        };
        if (model.selected) {
            rootClasses["[&_polygon]:stroke-sky-500"] = true;
            rootClasses["[&_polygon]:stroke-2"] = true;
        }

        return svg("g", { class: rootClasses }, diamond);
    }
}
