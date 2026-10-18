import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeView, type GNode } from "@mdeo/editor-shared";
import type { GCallNode } from "../model/callNode.js";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for call nodes: a box with a double left edge (a call, in the activity-diagram style of
 * story diagrams) holding the call's text.
 */
@injectable()
export class GCallNodeView extends GNodeView {
    protected override renderForeignElement(
        model: Readonly<GNode>,
        _context: RenderingContext,
        _children: readonly GModelElement[]
    ): VNode {
        const callModel = model as GCallNode;
        return html(
            "div",
            {
                class: {
                    "border-2": true,
                    "border-foreground": true,
                    "box-border": true,
                    "cursor-pointer": true,
                    "bg-background": true
                },
                style: {
                    borderLeftWidth: "6px",
                    borderLeftStyle: "double",
                    borderRadius: "6px",
                    padding: "4px 10px",
                    fontFamily: "monospace",
                    fontSize: "12px",
                    whiteSpace: "nowrap"
                }
            },
            callModel.text ?? ""
        );
    }
}
