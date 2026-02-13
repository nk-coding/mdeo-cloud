import type { RenderingContext } from "@eclipse-glsp/sprotty";
import {
    sharedImport,
    GEdgeView,
    type EdgeMarkerData,
    type EdgeAttachment,
    EdgeAttachmentPosition
} from "@mdeo/editor-shared";
import type { GLinkEdge } from "../model/linkEdge.js";
import { GLinkEndNode } from "../model/linkEndNode.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering link edges between objects.
 * Renders edges as polylines with an arrow marker at the target end and labels at source/target ends.
 */
@injectable()
export class GLinkEdgeView extends GEdgeView {
    /**
     * Gets edge attachments for link edges including source and target label nodes.
     *
     * @param model The link edge model
     * @param context The rendering context
     * @returns An array of edge attachments
     */
    protected override getEdgeAttachments(model: Readonly<GLinkEdge>, context: RenderingContext): EdgeAttachment[] {
        const attachments: EdgeAttachment[] = [];

        for (const child of model.children) {
            if (child instanceof GLinkEndNode) {
                const attachment: EdgeAttachment = {
                    vnode: context.renderElement(child),
                    bounds: child.bounds,
                    position:
                        child.end === "source" ? EdgeAttachmentPosition.SOURCE_LEFT : EdgeAttachmentPosition.TARGET_LEFT
                };
                attachments.push(attachment);
            }
        }

        return attachments;
    }

    /**
     * Renders the target marker as an arrow indicating the link direction.
     *
     * @param _model - The link edge model
     * @param _context - The rendering context
     * @returns Edge marker data with arrow shape
     */
    protected override renderTargetMarker(
        _model: Readonly<GLinkEdge>,
        _context: RenderingContext
    ): EdgeMarkerData | undefined {
        const arrow = svg("polygon", {
            class: {
                "fill-foreground": true,
                "stroke-foreground": true,
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
