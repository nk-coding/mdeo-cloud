import type { RenderingContext } from "@eclipse-glsp/sprotty";
import {
    sharedImport,
    GEdgeView,
    type EdgeMarkerData,
    type GEdge,
    type EdgeAttachment,
    EdgeAttachmentPosition
} from "@mdeo/editor-shared";
import { GControlFlowLabelNode } from "../model/controlFlowLabelNode.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering control flow edges.
 * Renders edges with an arrow marker at the target end.
 */
@injectable()
export class GControlFlowEdgeView extends GEdgeView {
    protected override getEdgeAttachments(model: Readonly<GEdge>, context: RenderingContext): EdgeAttachment[] {
        const attachments: EdgeAttachment[] = [];

        for (const child of model.children) {
            if (child instanceof GControlFlowLabelNode) {
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

    protected override renderTargetMarker(): EdgeMarkerData {
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
