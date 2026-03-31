import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import type { GNode } from "@mdeo/editor-shared";
import { sharedImport, GSimpleNodeView } from "@mdeo/editor-shared";
import type { GAssociationPropertyNode } from "../model/associationPropertyNode.js";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering association property nodes.
 */
@injectable()
export class GAssociationPropertyNodeView extends GSimpleNodeView {
    protected override renderNodeContent(
        model: Readonly<GAssociationPropertyNode>,
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
