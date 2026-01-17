import type { Action } from "@eclipse-glsp/sprotty";
import type { UpdateRoutingInformationOperation } from "@mdeo/editor-protocol";
import type { SModelElementImpl as GModelElement, SModelRootImpl as GModelRoot } from "sprotty";
import type { GEdge } from "../../model/edge.js";
import { EdgeRouter } from "../edge-rourting/edgeRouter.js";
import type { EdgeEditTool } from "./edgeEditTool.js";
import { isEdgeWithMeta } from "./util.js";
import { sharedImport } from "../../sharedImport.js";
import type { ISelectionListener } from "@eclipse-glsp/client";

const { DragAwareMouseListener, cursorFeedbackAction, findParentByFeature, isSelected } =
    sharedImport("@eclipse-glsp/client");

/**
 * CSS class for edge segment move cursor.
 */
const CSS_EDGE_SEGMENT_MOVE = "edge-segment-move-mode";
/**
 * Listener that handles edge selection and route editing.
 */

export class EdgeEditListener extends DragAwareMouseListener implements ISelectionListener {
    /**
     * The currently selected edge.
     */
    protected edge?: GEdge;

    /**
     * Edge router for computing routes.
     */
    protected edgeRouter: EdgeRouter;

    /**
     * Feedback emitter for cursor changes.
     */
    protected cursorFeedback;

    constructor(protected tool: EdgeEditTool) {
        super();
        this.edgeRouter = new EdgeRouter();
        this.cursorFeedback = this.tool.createFeedbackEmitter();
    }

    /**
     * Checks if the given edge is valid for editing.
     */
    protected isValidEdge(edge?: GEdge): edge is GEdge {
        return edge !== undefined && isSelected(edge);
    }

    /**
     * Sets the edge as selected for editing.
     */
    protected setEdgeSelected(edge: GEdge): void {
        this.edge = edge;
        this.tool.registerFeedbackListeners();
    }

    /**
     * Checks if an edge is currently selected.
     */
    protected isEdgeSelected(): boolean {
        return this.edge !== undefined && isSelected(this.edge);
    }

    override mouseDown(target: GModelElement, event: MouseEvent): Action[] {
        const result: Action[] = super.mouseDown(target, event);

        if (event.button === 0) {
            const eventTarget = event.target;
            if (eventTarget instanceof SVGElement) {
                const edgeId = eventTarget.dataset["edgeId"];

                if (edgeId !== undefined) {
                    const element = target.root.index.getById(edgeId);

                    if (isEdgeWithMeta(element)) {
                        if (!this.isEdgeSelected() || this.edge?.id !== element.id) {
                            this.dispose();
                            this.setEdgeSelected(element);
                        }
                        return result;
                    }
                }
            }

            const edge = findParentByFeature(target, isEdgeWithMeta);
            if (edge && this.isValidEdge(edge)) {
                if (!this.isEdgeSelected() || this.edge?.id !== edge.id) {
                    this.dispose();
                    this.setEdgeSelected(edge);
                }
            }
        } else if (event.button === 2) {
            this.dispose();
        }

        return result;
    }

    override draggingMouseMove(target: GModelElement, event: MouseEvent): Action[] {
        return super.draggingMouseMove(target, event);
    }

    override mouseUp(target: GModelElement, event: MouseEvent): Action[] {
        const result = super.mouseUp(target, event);

        const feedbackListener = this.tool.getFeedbackMovingListener();
        if (feedbackListener?.isTrackingDrag() && feedbackListener.getEdge()) {
            const edge = feedbackListener.getEdge()!;

            const routingUpdate = {
                elementId: edge.id,
                routingPoints: edge.meta?.routingPoints,
                sourceAnchor: edge.meta?.sourceAnchor,
                targetAnchor: edge.meta?.targetAnchor
            };
            const operation: UpdateRoutingInformationOperation = {
                kind: "updateRoutingInformation",
                isOperation: true,
                updates: [routingUpdate]
            };
            result.push(operation);
        }

        return result;
    }

    override mouseOver(target: GModelElement, event: MouseEvent): Action[] {
        const eventTarget = event.target;

        if (eventTarget instanceof SVGElement) {
            const segmentIndex = eventTarget.dataset["edgeSegmentIndex"];
            const edgeId = eventTarget.dataset["edgeId"];

            if (segmentIndex !== undefined && edgeId !== undefined) {
                const element = target.root.index.getById(edgeId);

                if (isEdgeWithMeta(element) && isSelected(element)) {
                    this.cursorFeedback
                        .add(cursorFeedbackAction(CSS_EDGE_SEGMENT_MOVE), cursorFeedbackAction())
                        .submit();
                    return [];
                }
            }
        }

        this.cursorFeedback.add(cursorFeedbackAction(), cursorFeedbackAction()).submit();
        return [];
    }

    selectionChanged(root: Readonly<GModelRoot>, selectedElements: string[]): void {
        if (this.edge) {
            if (selectedElements.indexOf(this.edge.id) > -1) {
                return;
            }

            for (const elementId of selectedElements.reverse()) {
                const element = root.index.getById(elementId);
                if (isEdgeWithMeta(element) && isSelected(element)) {
                    this.setEdgeSelected(element);
                    return;
                }
            }

            this.dispose();
        }
    }

    override dispose(): void {
        this.edge = undefined;
        this.cursorFeedback.dispose();
        this.tool.deregisterFeedbackListeners();
        super.dispose();
    }
}
