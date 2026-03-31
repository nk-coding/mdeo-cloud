import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GSimpleNodeView, type GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering pattern link end nodes (source or target).
 */
@injectable()
export class GPatternLinkEndNodeView extends GSimpleNodeView {
    protected override renderNodeContent(model: Readonly<GNode>, context: RenderingContext, children: readonly GModelElement[]): VNode[] {
        return children.map((child) => context.renderElement(child)).filter((v): v is VNode => v !== undefined);
    }

    protected override getClasses(_model: Readonly<GNode>): Record<string, boolean> {
        return {
            ...super.getClasses(_model),
            "p-0.75": true
        };
    }
}
