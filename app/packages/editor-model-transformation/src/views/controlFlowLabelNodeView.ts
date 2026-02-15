import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GSimpleNodeView, type GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");
const { selectFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering control flow label nodes.
 * Wraps the control flow label to provide proper bounds handling.
 */
@injectable()
export class GControlFlowLabelNodeView extends GSimpleNodeView {
    /**
     * Renders the content inside the control flow label node.
     * Renders children without selection feature enabled.
     *
     * @param model The control flow label node model
     * @param context The rendering context
     * @returns An array of VNodes representing the node's children
     */
    protected override renderNodeContent(model: Readonly<GNode>, context: RenderingContext): VNode[] {
        return context.renderChildren(model, { [selectFeature]: false });
    }

    /**
     * Returns the CSS classes to be applied to the control flow label node.
     *
     * @param model The node model being rendered
     * @returns An object mapping class names to boolean values
     */
    protected override getClasses(_model: Readonly<GNode>): Record<string, boolean> {
        return {
            ...super.getClasses(_model),
            "p-0.75": true
        };
    }
}
