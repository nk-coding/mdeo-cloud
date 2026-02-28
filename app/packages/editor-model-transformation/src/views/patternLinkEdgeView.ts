import type { Point, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport, GEdgeView, type EdgeAttachment, EdgeAttachmentPosition, type GEdge } from "@mdeo/editor-shared";
import type { GPatternLinkEdge } from "../model/patternLinkEdge.js";
import { GPatternLinkEndNode } from "../model/patternLinkEndNode.js";
import { GPatternLinkModifierLabel } from "../model/patternLinkModifierLabel.js";
import { PatternModifierKind } from "../model/elementTypes.js";
import type { VNode } from "snabbdom";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Gets CSS classes for stroke based on the modifier kind.
 *
 * @param modifier The pattern modifier kind
 * @returns CSS class string for the stroke color
 */
function getStrokeClassForModifier(modifier: PatternModifierKind): string {
    switch (modifier) {
        case PatternModifierKind.CREATE:
            return "stroke-create";
        case PatternModifierKind.DELETE:
            return "stroke-delete";
        case PatternModifierKind.FORBID:
            return "stroke-forbid";
        default:
            return "stroke-foreground";
    }
}

/**
 * View for rendering pattern link edges between instances.
 * Renders edges as polylines with labels at source/target ends.
 * The edge color changes based on the modifier (create/delete/forbid).
 */
@injectable()
export class GPatternLinkEdgeView extends GEdgeView {
    protected override getEdgeAttachments(model: Readonly<GEdge>, context: RenderingContext): EdgeAttachment[] {
        const attachments: EdgeAttachment[] = [];

        for (const child of model.children) {
            if (child instanceof GPatternLinkEndNode) {
                const attachment: EdgeAttachment = {
                    vnode: context.renderElement(child),
                    bounds: child.bounds,
                    position:
                        child.end === "source" ? EdgeAttachmentPosition.SOURCE_LEFT : EdgeAttachmentPosition.TARGET_LEFT
                };
                attachments.push(attachment);
            } else if (child instanceof GPatternLinkModifierLabel) {
                const attachment: EdgeAttachment = {
                    vnode: context.renderElement(child),
                    bounds: child.bounds,
                    position: EdgeAttachmentPosition.MIDDLE_LEFT
                };
                attachments.push(attachment);
            }
        }

        return attachments;
    }

    protected override renderVisiblePath(route: Point[], model: Readonly<GEdge>): VNode {
        const pathData = this.createPathData(route);

        return svg("path", {
            class: {
                [getStrokeClassForModifier((model as GPatternLinkEdge).modifier)]: true,
                "fill-none": true,
                "stroke-[1.5px]": true
            },
            attrs: {
                d: pathData
            }
        });
    }
}
