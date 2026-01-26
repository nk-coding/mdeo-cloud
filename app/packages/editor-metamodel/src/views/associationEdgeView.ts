import {
    sharedImport,
    GEdgeView,
    type EdgeAttachment,
    EdgeAttachmentPosition,
    type EdgeMarkerData
} from "@mdeo/editor-shared";
import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { GAssociationEdge } from "../model/associationEdge.js";
import { GAssociationPropertyNode } from "../model/associationPropertyNode.js";
import { GAssociationMultiplicityNode } from "../model/associationMultiplicityNode.js";
import { AssociationEndKind } from "../model/elementTypes.js";
import type { GNode } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering association edges.
 * Renders edges as polylines with association styling and attachment labels for multiplicity and properties.
 * Supports rendering composition diamonds and navigability arrows at edge ends.
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

    /**
     * Renders the source marker based on the sourceKind property.
     *
     * @param model The association edge model
     * @param context The rendering context
     * @returns Edge marker data for the source end
     */
    protected override renderSourceMarker(
        model: Readonly<GAssociationEdge>,
        _context: RenderingContext
    ): EdgeMarkerData | undefined {
        return this.createMarkerForKind(model.sourceKind, true);
    }

    /**
     * Renders the target marker based on the targetKind property.
     *
     * @param model The association edge model
     * @param context The rendering context
     * @returns Edge marker data for the target end
     */
    protected override renderTargetMarker(
        model: Readonly<GAssociationEdge>,
        _context: RenderingContext
    ): EdgeMarkerData | undefined {
        return this.createMarkerForKind(model.targetKind, false);
    }

    /**
     * Creates marker data for the given end kind.
     *
     * @param kind The kind of decoration to render
     * @param isSource Whether this is the source end (affects orientation)
     * @returns Edge marker data or undefined if no marker is needed
     */
    private createMarkerForKind(kind: AssociationEndKind, isSource: boolean): EdgeMarkerData | undefined {
        if (kind === AssociationEndKind.COMPOSITION) {
            return this.createDiamondMarker(isSource);
        }
        if (kind === AssociationEndKind.ARROW) {
            return this.createArrowMarker(isSource);
        }
        return undefined;
    }

    /**
     * Creates a filled diamond marker for composition.
     *
     * @param isSource Whether this is the source end
     * @returns Edge marker data with diamond shape
     */
    private createDiamondMarker(isSource: boolean): EdgeMarkerData {
        const points = isSource ? "0,0 10,-6 20,0 10,6" : "0,0 -10,-6 -20,0 -10,6";
        const diamond = svg("polygon", {
            class: {
                "fill-foreground": true,
                "stroke-foreground": true,
                "stroke-[1px]": true
            },
            attrs: {
                points
            }
        });

        return {
            marker: diamond,
            strokeOffset: 20,
            elementOffset: 14
        };
    }

    /**
     * Creates an arrow marker for navigability.
     *
     * @param isSource Whether this is the source end
     * @returns Edge marker data with arrow shape
     */
    private createArrowMarker(isSource: boolean): EdgeMarkerData {
        const points = isSource ? "0,0 12,-6 12,6" : "0,0 -12,-6 -12,6";
        const arrow = svg("polygon", {
            class: {
                "fill-foreground": true,
                "stroke-foreground": true,
                "stroke-[1px]": true
            },
            attrs: {
                points
            }
        });

        return {
            marker: arrow,
            strokeOffset: 12,
            elementOffset: 6
        };
    }
}
