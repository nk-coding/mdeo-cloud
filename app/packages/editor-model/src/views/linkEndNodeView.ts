import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import type { GNode } from "@mdeo/editor-shared";
import { sharedImport, GSimpleNodeView } from "@mdeo/editor-shared";
import type { GLinkEndNode } from "../model/linkEndNode.js";

const { injectable } = sharedImport("inversify");
const { selectFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering link end nodes (source or target).
 */
@injectable()
export class GLinkEndNodeView extends GSimpleNodeView {
    protected override renderNodeContent(model: Readonly<GLinkEndNode>, context: RenderingContext): VNode[] {
        return context.renderChildren(model, { [selectFeature]: false });
    }

    protected override getClasses(_model: Readonly<GNode>): Record<string, boolean> {
        return {
            ...super.getClasses(_model),
            "p-0.75": true
        };
    }
}
