import type { VNode } from "snabbdom";
import { sharedImport } from "../sharedImport.js";
import { NodeView } from "./nodeView.js";
import type { GRectangularNode } from "../model/rectangularNode.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export abstract class RectangularNodeView extends NodeView {
    /**
     * Renders the rectangle of the node
     *
     * @param model The model of the node
     * @returns The rectangle vnode
     */
    protected renderRect(model: Readonly<GRectangularNode>): VNode {
        return svg("rect", {
            class: {
                "stroke-foreground": true,
                "stroke-2": true,
                "fill-transparent": true
            },
            attrs: {
                x: model.bounds.x + 1,
                y: model.bounds.y + 1,
                width: Math.max(0, model.bounds.width - 2),
                height: Math.max(0, model.bounds.height - 2)
            }
        });
    }
}
