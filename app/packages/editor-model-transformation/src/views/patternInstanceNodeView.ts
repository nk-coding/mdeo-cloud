import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import { GRectangularNodeView, sharedImport } from "@mdeo/editor-shared";
import type { GRectangularNode } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";
import type { GPatternInstanceNode } from "../model/patternInstanceNode.js";
import { PatternModifierKind } from "@mdeo/protocol-model-transformation";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering pattern instance node elements in model transformation diagrams.
 * Extends GRectangularNodeView to render as an HTML-based bordered box, mirroring the
 * object node view used in the model editor.
 *
 * Modifier-specific border colours:
 *  - CREATE  → green border
 *  - DELETE  → red border
 *  - FORBID  → blue border
 *  - REQUIRE → orange border
 *  - NONE    → foreground (default) border
 *
 * Referenced instances (from previous matches) are rendered with reduced opacity.
 * Selection and resize handles are provided by the base GNodeView.
 */
@injectable()
export class GPatternInstanceNodeView extends GRectangularNodeView {
    /**
     * Returns CSS classes for the node container.
     * Applies modifier-based border colours and dims referenced instances.
     */
    protected override getClasses(model: Readonly<GRectangularNode>): Record<string, boolean> {
        const instanceModel = model as GPatternInstanceNode;
        const modifier = instanceModel.modifier;

        return {
            "border-2": true,
            "border-foreground": modifier === PatternModifierKind.NONE,
            "border-create": modifier === PatternModifierKind.CREATE,
            "border-delete": modifier === PatternModifierKind.DELETE,
            "border-forbid": modifier === PatternModifierKind.FORBID,
            "border-require": modifier === PatternModifierKind.REQUIRE,
            "bg-background": true,
            "box-border": true,
            flex: true,
            "flex-col": true,
            "w-fit": true,
            "cursor-pointer": true
        };
    }

    /**
     * Renders the content inside the pattern instance node by rendering all child
     * compartments (modifier title, header, properties).
     */
    protected override renderNodeContent(
        model: Readonly<GRectangularNode>,
        context: RenderingContext,
        children: readonly GModelElement[]
    ): VNode[] {
        return children.map((child) => context.renderElement(child)).filter((v): v is VNode => v !== undefined);
    }
}
