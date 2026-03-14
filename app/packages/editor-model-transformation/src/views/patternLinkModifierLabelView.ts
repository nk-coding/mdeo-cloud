import { GLabelView, sharedImport } from "@mdeo/editor-shared";
import type { GLabel } from "@mdeo/editor-shared";
import { PatternModifierKind } from "@mdeo/protocol-model-transformation";
import { isPatternInstanceNode } from "../model/patternInstanceNode.js";

const { injectable } = sharedImport("inversify");
const { findParentByFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for the pattern link modifier label.
 * Extends GLabelView to add modifier-specific colour classes
 * (text-create, text-delete, text-forbid, text-require) derived from the text content.
 * The parent GPatternLinkModifierNode handles positioning on the edge.
 */
@injectable()
export class GPatternLinkModifierLabelView extends GLabelView {
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        const parentInstance = findParentByFeature(model, isPatternInstanceNode);
        return {
            ...super.getClasses(model),
            "text-create": parentInstance?.modifier === PatternModifierKind.CREATE,
            "text-delete": parentInstance?.modifier === PatternModifierKind.DELETE,
            "text-forbid": parentInstance?.modifier === PatternModifierKind.FORBID,
            "text-require": parentInstance?.modifier === PatternModifierKind.REQUIRE
        };
    }
}
