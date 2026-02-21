import type { IView } from "@eclipse-glsp/sprotty";
import { sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";
import type { GPatternLinkModifierLabel } from "../model/patternLinkModifierLabel.js";
import { PatternModifierKind } from "../model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { svg, html, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for the pattern link modifier label.
 * Renders a SVG foreignObject containing an HTML span with french-quote–wrapped
 * modifier text (e.g. «create», «delete», «forbid»), coloured to match the
 * instance/edge colour scheme (`text-create`, `text-delete`, `text-forbid`).
 *
 * No selection, resize, or move handles are rendered because the model element
 * carries no node features.
 */
@injectable()
export class GPatternLinkModifierLabelView implements IView {
    render(model: Readonly<GPatternLinkModifierLabel>): VNode | undefined {
        const modifier = model.modifier;
        if (modifier === PatternModifierKind.NONE) {
            return undefined;
        }

        const text = `\u00ab${modifier}\u00bb`;

        return svg(
            "foreignObject",
            {
                class: {
                    "pointer-events-none": true,
                    "[&_*]:pointer-events-auto": true
                },
                attrs: {
                    width: 99999,
                    height: 99999
                }
            },

            html(
                "span",
                {
                    class: {
                        "text-create": modifier === PatternModifierKind.CREATE,
                        "text-delete": modifier === PatternModifierKind.DELETE,
                        "text-forbid": modifier === PatternModifierKind.FORBID
                    },
                    attrs: {
                        [ATTR_BBOX_ELEMENT]: true
                    }
                },
                text
            )
        );
    }
}
