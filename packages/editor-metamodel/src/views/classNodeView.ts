import { type RenderingContext } from "@eclipse-glsp/sprotty";
import { GRectangularNodeView, sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";
import type { GClassNode } from "../model/classNode.js";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering Class node elements in metamodel diagrams.
 * Extends RectangularNodeView to provide a bordered rectangular container.
 */
@injectable()
export class GClassNodeView extends GRectangularNodeView {
    /**
     * Renders the content inside the class node by rendering all child elements.
     *
     * @param model - The class node model being rendered
     * @param context - The rendering context
     * @param args - Optional additional arguments
     * @returns An array of VNodes representing the node's children
     */
    protected renderNodeContent(model: Readonly<GClassNode>, context: RenderingContext, args?: {}): VNode[] {
        return context.renderChildren(model);
    }
}
