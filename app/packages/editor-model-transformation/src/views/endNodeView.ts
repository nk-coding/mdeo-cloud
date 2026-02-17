import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "@mdeo/editor-shared";
import type { GEndNode } from "../model/endNode.js";
import { EndNodeKind } from "../model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering end nodes.
 * Renders differently based on kind:
 * - STOP: Filled circle with hollow outer ring (bullseye)
 * - KILL: X mark (cross)
 */
@injectable()
export class GEndNodeView implements IView {
    /**
     * The outer radius of the end node
     */
    static readonly OUTER_RADIUS = 14;

    /**
     * The inner radius for stop nodes (bullseye)
     */
    static readonly INNER_RADIUS = 8;

    /**
     * The size of the kill node
     */
    static readonly KILL_SIZE = 24;

    /**
     * Renders the end node based on its kind.
     *
     * @param model The end node model
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GEndNode>, _context: RenderingContext): VNode {
        const rootClasses: Record<string, boolean> = {
            "cursor-pointer": true
        };
        if (model.selected) {
            rootClasses["[&_circle]:stroke-sky-500"] = true;
            rootClasses["[&_line]:stroke-sky-500"] = true;
        }

        if (model.kind === EndNodeKind.KILL) {
            return svg("g", { class: rootClasses }, ...this.renderKillNode());
        } else {
            return svg("g", { class: rootClasses }, ...this.renderStopNode());
        }
    }

    /**
     * Renders a stop node as a bullseye (filled circle with outer ring).
     *
     * @returns Array of VNodes for the stop node
     */
    private renderStopNode(): VNode[] {
        const outerRadius = GEndNodeView.OUTER_RADIUS;
        const innerRadius = GEndNodeView.INNER_RADIUS;
        const center = outerRadius;

        const outerCircle = svg("circle", {
            class: {
                "fill-none": true,
                "stroke-foreground": true,
                "stroke-2": true
            },
            attrs: {
                cx: center,
                cy: center,
                r: outerRadius
            }
        });

        const innerCircle = svg("circle", {
            class: {
                "fill-foreground": true,
                "stroke-foreground": true
            },
            attrs: {
                cx: center,
                cy: center,
                r: innerRadius
            }
        });

        return [outerCircle, innerCircle];
    }

    /**
     * Renders a kill node as an X mark (cross).
     *
     * @returns Array of VNodes for the kill node
     */
    private renderKillNode(): VNode[] {
        const size = GEndNodeView.KILL_SIZE;
        const padding = 4;

        const line1 = svg("line", {
            class: {
                "stroke-foreground": true,
                "stroke-[3px]": true
            },
            attrs: {
                x1: padding,
                y1: padding,
                x2: size - padding,
                y2: size - padding,
                "stroke-linecap": "round"
            }
        });

        const line2 = svg("line", {
            class: {
                "stroke-foreground": true,
                "stroke-[3px]": true
            },
            attrs: {
                x1: size - padding,
                y1: padding,
                x2: padding,
                y2: size - padding,
                "stroke-linecap": "round"
            }
        });

        return [line1, line2];
    }
}
