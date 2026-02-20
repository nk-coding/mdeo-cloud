import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { GLabel } from "@mdeo/editor-shared";
import { sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering pattern instance name labels in model transformation diagrams.
 * Renders the combined instance name and optional type with underline styling
 * (UML convention for instances). Uses HTML since the pattern instance node is
 * now rendered as a foreign-object HTML box.
 */
@injectable()
export class GPatternInstanceLabelView implements IView {
    /**
     * Renders the pattern instance label as an underlined HTML span.
     *
     * @param model The label model being rendered
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GLabel>, _context: RenderingContext): VNode {
        const text = model.text ?? "";

        return html(
            "span",
            {
                class: {
                    "text-sm": true,
                    underline: true,
                    "text-center": true,
                    block: true
                }
            },
            text
        );
    }
}
