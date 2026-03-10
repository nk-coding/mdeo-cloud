import { GHorizontalDividerView, sharedImport } from "@mdeo/editor-shared";
import type { GHorizontalDivider } from "@mdeo/editor-shared";
import { PatternModifierKind } from "../model/elementTypes.js";
import { isPatternInstanceNode } from "../model/patternInstanceNode.js";

const { injectable } = sharedImport("inversify");
const { findParentByFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Horizontal divider view for use inside pattern instance nodes.
 * Colours the divider line using the same colour as the enclosing
 * GPatternInstanceNode border so the separator visually matches the
 * modifier colour (green for create, red for delete, blue for forbid,
 * orange for require,
 * foreground for none).
 */
@injectable()
export class GPatternInstanceDividerView extends GHorizontalDividerView {
    protected override getClasses(model: Readonly<GHorizontalDivider>): Record<string, boolean> {
        const parentInstance = findParentByFeature(model, isPatternInstanceNode);

        return {
            "w-full": true,
            "border-b-2": true,
            "border-create": parentInstance?.modifier === PatternModifierKind.CREATE,
            "border-delete": parentInstance?.modifier === PatternModifierKind.DELETE,
            "border-forbid": parentInstance?.modifier === PatternModifierKind.FORBID,
            "border-require": parentInstance?.modifier === PatternModifierKind.REQUIRE,
            "border-foreground": parentInstance?.modifier === PatternModifierKind.NONE
        };
    }
}
