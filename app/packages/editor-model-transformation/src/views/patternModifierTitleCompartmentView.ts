import { GCompartmentView, sharedImport } from "@mdeo/editor-shared";
import type { GPatternModifierTitleCompartment } from "../model/patternModifierTitleCompartment.js";
import type { VNode } from "snabbdom";
import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { PatternModifierKind } from "../model/elementTypes.js";
import { isPatternInstanceNode } from "../model/patternInstanceNode.js";

const { injectable } = sharedImport("inversify");
const { html, findParentByFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for the pattern modifier title compartment.
 * Renders a UML stereotype label (e.g. «create», «delete», «forbid») above the instance
 * name label. The modifier kind is resolved by traversing up to the closest ancestor
 * GPatternInstanceNode rather than storing it in the compartment model itself.
 * The stereotype text is coloured using the same colour used for the node's border.
 */
@injectable()
export class GPatternModifierTitleCompartmentView extends GCompartmentView {
    override render(model: Readonly<GPatternModifierTitleCompartment>, context: RenderingContext): VNode | undefined {
        const parentInstance = findParentByFeature(model, isPatternInstanceNode);
        const modifier = parentInstance != null ? parentInstance.modifier : PatternModifierKind.NONE;

        return html(
            "div",
            {
                class: this.getClasses(model)
            },
            html(
                "span",
                {
                    class: {
                        "text-center": true,
                        "text-create": modifier === PatternModifierKind.CREATE,
                        "text-delete": modifier === PatternModifierKind.DELETE,
                        "text-forbid": modifier === PatternModifierKind.FORBID,
                        "text-foreground": modifier === PatternModifierKind.NONE
                    }
                },
                `\u00ab${modifier}\u00bb`
            ),
            ...context.renderChildren(model)
        );
    }
}
