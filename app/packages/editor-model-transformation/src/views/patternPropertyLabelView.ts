import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import type { GLabel } from "@mdeo/editor-shared";
import { sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering pattern property labels in model transformation diagrams.
 * Uses HTML since the pattern instance node is now rendered as a foreign-object HTML box.
 */
@injectable()
export class GPatternPropertyLabelView implements IView {
    /**
     * Renders the pattern property label as an HTML span.
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
                    "text-sm": true
                }
            },
            text
        );
    }
}
