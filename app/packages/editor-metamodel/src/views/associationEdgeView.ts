import { sharedImport, GEdgeView, type EdgeAttachment, EdgeAttachmentPosition } from "@mdeo/editor-shared";
import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { GAssociationEdge } from "../model/associationEdge.js";
import { GAssociationPropertyNode } from "../model/associationPropertyNode.js";
import { GAssociationMultiplicityNode } from "../model/associationMultiplicityNode.js";
import type { GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering association edges.
 * Renders edges as polylines with association styling and attachment labels for multiplicity and properties.
 */
@injectable()
export class GAssociationEdgeView extends GEdgeView {
    /**
     * Gets edge attachments for association edges including multiplicity and property labels.
     *
     * @param model The association edge model
     * @param context The rendering context
     * @returns An array of edge attachments
     */
    protected override getEdgeAttachments(
        model: Readonly<GAssociationEdge>,
        context: RenderingContext
    ): EdgeAttachment[] {
        const attachments: EdgeAttachment[] = [];

        for (const child of model.children) {
            if (child instanceof GAssociationPropertyNode) {
                const attachment = this.createAttachmentFromNode(
                    child,
                    child.end == "source" ? EdgeAttachmentPosition.SOURCE_LEFT : EdgeAttachmentPosition.TARGET_LEFT,
                    context
                );
                if (attachment) {
                    attachments.push(attachment);
                }
            } else if (child instanceof GAssociationMultiplicityNode) {
                const attachment = this.createAttachmentFromNode(
                    child,
                    child.end == "source" ? EdgeAttachmentPosition.SOURCE_RIGHT : EdgeAttachmentPosition.TARGET_RIGHT,
                    context
                );
                if (attachment) {
                    attachments.push(attachment);
                }
            }
        }

        return attachments;
    }

    /**
     * Creates an edge attachment from a node (property or multiplicity).
     *
     * @param node The node to create attachment from
     * @param position The position of the attachment on the edge
     * @param context The rendering context
     * @returns An edge attachment or undefined if the node cannot be rendered
     */
    protected createAttachmentFromNode(
        node: GNode,
        position: EdgeAttachmentPosition,
        context: RenderingContext
    ): EdgeAttachment | undefined {
        return {
            vnode: context.renderElement(node),
            bounds: node.bounds,
            position
        };
    }
}
