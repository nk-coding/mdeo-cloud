import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import type { GNode } from "@mdeo/editor-shared";
import { sharedImport, GSimpleNodeView } from "@mdeo/editor-shared";
import type { GAssociationMultiplicityNode } from "../model/associationMultiplicityNode.js";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering association multiplicity nodes.
 */
@injectable()
export class GAssociationMultiplicityNodeView extends GSimpleNodeView {
    protected override renderNodeContent(
        model: Readonly<GAssociationMultiplicityNode>,
        context: RenderingContext,
        children: readonly GModelElement[]
    ): VNode[] {
        return children.map((child) => context.renderElement(child)).filter((v): v is VNode => v !== undefined);
    }

    protected override getClasses(_model: Readonly<GNode>): Record<string, boolean> {
        return {
            ...super.getClasses(_model),
            "p-0.75": true
        };
    }
}
