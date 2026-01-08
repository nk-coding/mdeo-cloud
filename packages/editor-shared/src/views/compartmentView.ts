import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "../sharedImport.js";
import type { GCompartment } from "../model/compartment.js";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering compartment elements.
 * Renders a flex container with vertical layout and spacing between children.
 */
@injectable()
export class GCompartmentView {
    /**
     * Renders the compartment as a vertical flex container with margin and gap.
     *
     * @param model - The compartment model being rendered
     * @param context - The rendering context
     * @param args - Optional additional arguments
     * @returns A VNode representing the compartment
     */
    render(model: Readonly<GCompartment>, context: RenderingContext, args?: {}): VNode | undefined {
        return html(
            "div",
            {
                class: {
                    "m-2": true,
                    flex: true,
                    "flex-col": true,
                    "gap-1": true
                }
            },
            ...context.renderChildren(model)
        );
    }
}
