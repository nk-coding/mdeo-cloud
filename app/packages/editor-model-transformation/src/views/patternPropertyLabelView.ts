import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { GLabel } from "@mdeo/editor-shared";
import { sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering pattern property labels in model transformation diagrams.
 * Uses pure SVG text rendering since it's inside an SVG context (pattern instance node).
 */
@injectable()
export class GPatternPropertyLabelView implements IView {
    /**
     * Renders the pattern property label as SVG text.
     *
     * @param model The label model being rendered
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GLabel>, _context: RenderingContext): VNode {
        const text = model.text ?? "";

        return svg(
            "text",
            {
                class: {
                    "fill-foreground": true,
                    "text-sm": true,
                    "pointer-events-none": true
                },
                attrs: {
                    x: 0,
                    y: 0,
                    "text-anchor": "start",
                    "dominant-baseline": "hanging",
                    [ATTR_BBOX_ELEMENT]: true
                }
            },
            text
        );
    }
}
