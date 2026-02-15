import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "@mdeo/editor-shared";
import type { GStartNode } from "../model/startNode.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering start nodes.
 * Renders a filled black circle representing the start of a control flow, similar to UML activity diagrams.
 */
@injectable()
export class GStartNodeView implements IView {
    /**
     * The radius of the start node circle
     */
    static readonly RADIUS = 12;

    /**
     * Renders the start node as a filled black circle.
     *
     * @param model The start node model
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GStartNode>, _context: RenderingContext): VNode {
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

        const rootClasses: Record<string, boolean> = {
            "cursor-pointer": true
        };
        if (model.selected) {
            rootClasses["[&_circle]:stroke-sky-500"] = true;
            rootClasses["[&_circle]:stroke-2"] = true;
        }

        return svg("g", { class: rootClasses }, circle);
    }
}
