import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "@mdeo/editor-shared";
import type { GMatchNodeCompartments } from "../model/matchNodeCompartments.js";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering the match node compartments container.
 * Like GCompartmentView but without margin, so it fits flush inside the match node.
 * Renders its children (compartments and dividers) in a vertical flex column.
 */
@injectable()
export class GMatchNodeCompartmentsView {
    render(model: Readonly<GMatchNodeCompartments>, context: RenderingContext): VNode | undefined {
        return html(
            "div",
            {
                class: {
                    flex: true,
                    "flex-col": true
                }
            },
            ...context.renderChildren(model)
        );
    }
}
