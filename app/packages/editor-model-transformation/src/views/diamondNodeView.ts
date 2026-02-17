import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "@mdeo/editor-shared";
import type { GDiamondNode } from "../model/diamondNode.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering diamond nodes.
 * Renders a diamond shape with the expression text inside, used for if/while branching.
 */
@injectable()
export class GDiamondNodeView implements IView {
    /**
     * The width of the diamond
     */
    static readonly WIDTH = 80;

    /**
     * The height of the diamond
     */
    static readonly HEIGHT = 50;

    /**
     * Maximum characters to display before truncating
     */
    static readonly MAX_TEXT_LENGTH = 15;

    /**
     * Renders the diamond node with expression text inside.
     *
     * @param model The diamond node model
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GDiamondNode>, _context: RenderingContext): VNode {
        const width = GDiamondNodeView.WIDTH;
        const height = GDiamondNodeView.HEIGHT;
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

        // Truncate expression text if too long
        let displayText = model.expression ?? "";
        if (displayText.length > GDiamondNodeView.MAX_TEXT_LENGTH) {
            displayText = displayText.substring(0, GDiamondNodeView.MAX_TEXT_LENGTH - 1) + "…";
        }

        const text = svg(
            "text",
            {
                class: {
                    "fill-foreground": true,
                    "text-xs": true,
                    "pointer-events-none": true
                },
                attrs: {
                    x: halfWidth,
                    y: halfHeight,
                    "text-anchor": "middle",
                    "dominant-baseline": "middle"
                }
            },
            displayText
        );

        const rootClasses: Record<string, boolean> = {
            "cursor-pointer": true
        };
        if (model.selected) {
            rootClasses["[&_polygon]:stroke-sky-500"] = true;
            rootClasses["[&_polygon]:stroke-2"] = true;
        }

        return svg("g", { class: rootClasses }, diamond, text);
    }
}
