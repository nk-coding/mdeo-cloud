import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GSimpleNodeView, type GNode } from "@mdeo/editor-shared";
import type { GPatternLinkModifierNode } from "../model/patternLinkModifierNode.js";
import { PatternModifierKind } from "@mdeo/protocol-model-transformation";

const { injectable } = sharedImport("inversify");
const { selectFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering pattern link modifier nodes.
 * Renders a badge showing the modifier keyword (e.g. «create», «delete»)
 * with the appropriate colour class, positioned in the middle of the edge.
 */
@injectable()
export class GPatternLinkModifierNodeView extends GSimpleNodeView {
    protected override renderNodeContent(model: Readonly<GNode>, context: RenderingContext): VNode[] {
        return context.renderChildren(model, { [selectFeature]: false });
    }

    protected override getClasses(model: Readonly<GPatternLinkModifierNode>): Record<string, boolean> {
        const modifier = model.modifier;
        return {
            ...super.getClasses(model),
            "p-0.75": true,
            "text-create": modifier === PatternModifierKind.CREATE,
            "text-delete": modifier === PatternModifierKind.DELETE,
            "text-forbid": modifier === PatternModifierKind.FORBID,
            "text-require": modifier === PatternModifierKind.REQUIRE
        };
    }
}
