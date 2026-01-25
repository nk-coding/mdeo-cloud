import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { GRectangularNodeView, sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";
import type { GEnumNode } from "../model/enumNode.js";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering Enum node elements in metamodel diagrams.
 * Extends RectangularNodeView to provide a bordered rectangular container.
 */
@injectable()
export class GEnumNodeView extends GRectangularNodeView {
    /**
     * Renders the content inside the enum node by rendering all child elements.
     *
     * @param model The enum node model being rendered
     * @param context The rendering context
     * @returns An array of VNodes representing the node's children
     */
    protected renderNodeContent(model: Readonly<GEnumNode>, context: RenderingContext): VNode[] {
        return context.renderChildren(model);
    }
}
