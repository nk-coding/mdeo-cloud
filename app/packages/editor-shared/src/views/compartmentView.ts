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
    render(model: Readonly<GCompartment>, context: RenderingContext): VNode | undefined {
        return html(
            "div",
            {
                class: this.getClasses(model)
            },
            ...context.renderChildren(model)
        );
    }

    /**
     * Gets the CSS classes for the compartment container.
     *
     * @param _model The compartment model
     * @returns A record of CSS classes to apply
     */
    protected getClasses(_model: Readonly<GCompartment>): Record<string, boolean> {
        return {
            "m-2": true,
            flex: true,
            "flex-col": true,
            "gap-1": true
        };
    }
}
