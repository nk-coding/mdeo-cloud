import type { RenderingContext } from "@eclipse-glsp/sprotty";
import {
    sharedImport,
    GEdgeView,
    type EdgeMarkerData,
    type EdgeAttachment,
    EdgeAttachmentPosition,
    type GEdge
} from "@mdeo/editor-shared";
import type { GPatternLinkEdge } from "../model/patternLinkEdge.js";
import type { GPatternLinkEndNode } from "../model/patternLinkEndNode.js";
import { PatternModifierKind } from "../model/elementTypes.js";

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
            return "stroke-green-600";
        case PatternModifierKind.DELETE:
            return "stroke-red-600";
        case PatternModifierKind.FORBID:
            return "stroke-amber-600";
        default:
            return "stroke-foreground";
    }
}

/**
 * Gets CSS classes for fill based on the modifier kind.
 *
 * @param modifier The pattern modifier kind
 * @returns CSS class string for the fill color
 */
function getFillClassForModifier(modifier: PatternModifierKind): string {
    switch (modifier) {
        case PatternModifierKind.CREATE:
            return "fill-green-600";
        case PatternModifierKind.DELETE:
            return "fill-red-600";
        case PatternModifierKind.FORBID:
            return "fill-amber-600";
        default:
            return "fill-foreground";
    }
}

/**
 * View for rendering pattern link edges between instances.
 * Renders edges as polylines with an arrow marker at the target end and labels at source/target ends.
 * The edge color changes based on the modifier (create/delete/forbid).
 */
@injectable()
export class GPatternLinkEdgeView extends GEdgeView {
    /**
     * Gets edge attachments for pattern link edges including source and target label nodes.
     *
     * @param model The pattern link edge model
     * @param context The rendering context
     * @returns An array of edge attachments
     */
    protected override getEdgeAttachments(model: Readonly<GEdge>, context: RenderingContext): EdgeAttachment[] {
        const attachments: EdgeAttachment[] = [];

        for (const child of model.children) {
            // Check if child has 'end' property (is a GPatternLinkEndNode)
            const endNode = child as unknown as GPatternLinkEndNode;
            if (endNode.end !== undefined) {
                const attachment: EdgeAttachment = {
                    vnode: context.renderElement(child),
                    bounds: endNode.bounds,
                    position:
                        endNode.end === "source"
                            ? EdgeAttachmentPosition.SOURCE_LEFT
                            : EdgeAttachmentPosition.TARGET_LEFT
                };
                attachments.push(attachment);
            }
        }

        return attachments;
    }

    /**
     * Renders the target marker as an arrow indicating the link direction.
     * The arrow color matches the modifier styling.
     *
     * @param model The pattern link edge model
     * @param _context The rendering context
     * @returns Edge marker data with arrow shape
     */
    protected override renderTargetMarker(
        model: Readonly<GEdge>,
        _context: RenderingContext
    ): EdgeMarkerData | undefined {
        const patternModel = model as GPatternLinkEdge;
        const fillClass = getFillClassForModifier(patternModel.modifier);
        const strokeClass = getStrokeClassForModifier(patternModel.modifier);

        const arrow = svg("polygon", {
            class: {
                [fillClass]: true,
                [strokeClass]: true,
                "stroke-[1px]": true
            },
            attrs: {
                points: "0,0 -12,-6 -12,6"
            }
        });

        return {
            marker: arrow,
            strokeOffset: 12,
            elementOffset: 6
        };
    }
}
