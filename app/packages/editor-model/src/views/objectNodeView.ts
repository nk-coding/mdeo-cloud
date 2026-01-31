import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { GRectangularNodeView, sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";
import type { GObjectNode } from "../model/objectNode.js";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering Object node elements in model diagrams.
 * Extends GRectangularNodeView to provide a bordered rectangular container.
 */
@injectable()
export class GObjectNodeView extends GRectangularNodeView {
    /**
     * Renders the content inside the object node by rendering all child elements.
     *
     * @param model The object node model being rendered
     * @param context The rendering context
     * @returns An array of VNodes representing the node's children
     */
    protected renderNodeContent(model: Readonly<GObjectNode>, context: RenderingContext): VNode[] {
        return context.renderChildren(model);
    }
}
