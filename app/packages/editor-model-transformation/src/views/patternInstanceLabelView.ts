import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { GLabel } from "@mdeo/editor-shared";
import { sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering pattern instance name labels in model transformation diagrams.
 * Renders the combined instance name and optional type with underline styling (UML convention for instances).
 * Format: "name" or "name : type"
 * Uses pure SVG text rendering since it's inside an SVG context.
 */
@injectable()
export class GPatternInstanceLabelView implements IView {
    /**
     * Renders the pattern instance label as SVG text with underline.
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
                    "text-decoration": "underline",
                    [ATTR_BBOX_ELEMENT]: true
                }
            },
            text
        );
    }
}
