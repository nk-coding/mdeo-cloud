import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeView, type GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering start nodes.
 * Renders a filled circle representing the start of a control flow, similar to UML activity diagrams.
 * Uses an HTML div with border-radius so the bounding box correctly includes the border.
 */
@injectable()
export class GStartNodeView extends GNodeView {
    /**
     * The radius of the start node circle
     */
    static readonly RADIUS = 12;

    /**
     * Renders the start node as a filled circle using an HTML div.
     *
     * @param _model The node model
     * @param _context The rendering context
     * @returns The rendered VNode
     */
    protected override renderForeignElement(
        _model: Readonly<GNode>,
        _context: RenderingContext,
        _children: readonly GModelElement[]
    ): VNode {
        const diameter = GStartNodeView.RADIUS * 2;
        return html("div", {
            class: {
                "bg-foreground": true,
                "rounded-full": true,
                "box-border": true,
                "cursor-pointer": true
            },
            style: {
                width: `${diameter}px`,
                height: `${diameter}px`
            }
        });
    }
}
