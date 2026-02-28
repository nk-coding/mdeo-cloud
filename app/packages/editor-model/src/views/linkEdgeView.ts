import type { RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport, GEdgeView, type EdgeAttachment, EdgeAttachmentPosition } from "@mdeo/editor-shared";
import type { GLinkEdge } from "../model/linkEdge.js";
import { GLinkEndNode } from "../model/linkEndNode.js";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering link edges between objects.
 * Renders edges as polylines with labels at source/target ends.
 */
@injectable()
export class GLinkEdgeView extends GEdgeView {
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
}
