import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GSimpleNodeView, type GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering control flow label nodes.
 * Wraps the control flow label to provide proper bounds handling.
 */
@injectable()
export class GControlFlowLabelNodeView extends GSimpleNodeView {
    protected override renderNodeContent(
        model: Readonly<GNode>,
        context: RenderingContext,
        children: readonly GModelElement[]
    ): VNode[] {
        return children.map((child) => context.renderElement(child)).filter((v): v is VNode => v !== undefined);
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
