import type { Action, CommandExecutionContext, CommandReturn, GModelElementSchema } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import type { Point } from "@eclipse-glsp/protocol";
import type { EdgeAnchor } from "@mdeo/protocol-common";
import { GEdge } from "../../model/edge.js";
import { EdgeEditHighlightState, type EdgeEditHighlight } from "../../model/node.js";

const { injectable, inject } = sharedImport("inversify");
const { TYPES } = sharedImport("@eclipse-glsp/sprotty");
const { FeedbackCommand } = sharedImport("@eclipse-glsp/client");

/**
 * Action to start create-edge feedback.
 * Dispatched when the user selects a source node and the feedback edge should appear.
 */
export interface StartCreateEdgeFeedbackAction extends Action {
    kind: typeof StartCreateEdgeFeedbackAction.KIND;
    /**
     * The ID of the feedback edge to insert.
     */
    edgeId: string;
    /**
     * The ID of the parent container element into which the feedback edge should be inserted.
     * When provided, the edge is added to that container; otherwise it falls back to root.
     */
    parentId?: string;
    /**
     * The schema used to create the feedback edge model element.
     */
    schema: GModelElementSchema;
    /**
     * The initial position for the target end of the feedback edge.
     */
    position: Point;
    /**
     * The fixed source anchor selected at the start of creation.
     */
    sourceAnchor: EdgeAnchor;
}

export namespace StartCreateEdgeFeedbackAction {
    export const KIND = "startCreateEdgeFeedback";

    /**
     * Creates a new StartCreateEdgeFeedbackAction.
     *
     * @param edgeId The ID of the feedback edge
     * @param schema The schema template for the feedback edge
     * @param position The initial target-end position
     * @param sourceAnchor The fixed source anchor selected at start
     * @returns The action
     */
    export function create(
        edgeId: string,
        schema: GModelElementSchema,
        position: Point,
        sourceAnchor: EdgeAnchor,
        parentId?: string
    ): StartCreateEdgeFeedbackAction {
        return { kind: KIND, edgeId, parentId, schema, position, sourceAnchor };
    }
}

/**
 * Action to update create-edge feedback.
 * Dispatched as the mouse moves during the second phase of creation.
 */
export interface UpdateCreateEdgeFeedbackAction extends Action {
    kind: typeof UpdateCreateEdgeFeedbackAction.KIND;
    /**
     * The ID of the feedback edge.
     */
    edgeId: string;
    /**
     * The new free position of the target end (when not snapped).
     */
    position?: Point;
    /**
     * The anchor when snapped to a target node.
     */
    anchor?: EdgeAnchor;
    /**
     * The ID of the target element if hovering over a valid target.
     */
    targetId?: string;
    /**
     * An optional updated schema from the provider.
     * If set, the feedback edge model is replaced with this schema.
     */
    updatedSchema?: GModelElementSchema;
}

export namespace UpdateCreateEdgeFeedbackAction {
    export const KIND = "updateCreateEdgeFeedback";

    /**
     * Creates a new UpdateCreateEdgeFeedbackAction.
     *
     * @param edgeId The ID of the feedback edge
     * @param position The new free position (when not snapped)
     * @param anchor The anchor (when snapped to target)
     * @param targetId The target element ID if hovering over a valid target
     * @param updatedSchema Optional updated schema from the provider
     * @returns The action
     */
    export function create(
        edgeId: string,
        position?: Point,
        anchor?: EdgeAnchor,
        targetId?: string,
        updatedSchema?: GModelElementSchema
    ): UpdateCreateEdgeFeedbackAction {
        return { kind: KIND, edgeId, position, anchor, targetId, updatedSchema };
    }
}

/**
 * Action to stop create-edge feedback and remove the feedback edge.
 */
export interface StopCreateEdgeFeedbackAction extends Action {
    kind: typeof StopCreateEdgeFeedbackAction.KIND;
    /**
     * The ID of the feedback edge to remove.
     */
    edgeId: string;
}

export namespace StopCreateEdgeFeedbackAction {
    export const KIND = "stopCreateEdgeFeedback";

    /**
     * Creates a new StopCreateEdgeFeedbackAction.
     *
     * @param edgeId The ID of the feedback edge to remove
     * @returns The action
     */
    export function create(edgeId: string): StopCreateEdgeFeedbackAction {
        return { kind: KIND, edgeId };
    }
}

/**
 * Command to start create-edge feedback.
 * Creates the feedback edge from the schema and inserts it into the model.
 */
@injectable()
export class StartCreateEdgeFeedbackCommand extends FeedbackCommand {
    static readonly KIND = StartCreateEdgeFeedbackAction.KIND;

    constructor(@inject(TYPES.Action) protected action: StartCreateEdgeFeedbackAction) {
        super();
    }

    /**
     * Creates and inserts the feedback edge into the current root model.
     *
     * @param context The command execution context
     * @returns The updated model root
     */
    execute(context: CommandExecutionContext): CommandReturn {
        const edge = context.modelFactory.createElement(this.action.schema);
        if (edge instanceof GEdge) {
            edge.id = this.action.edgeId;
            edge.edgeCreateData = {
                sourceId: edge.sourceId,
                sourceAnchor: this.action.sourceAnchor,
                position: this.action.position
            };
            const parent = (
                this.action.parentId ? (context.root.index.getById(this.action.parentId) ?? context.root) : context.root
            ) as typeof context.root;
            parent.add(edge);
        }
        return context.root;
    }
}

/**
 * Command to update create-edge feedback.
 * Updates the feedback edge position/anchor and manages target node highlight state.
 */
@injectable()
export class UpdateCreateEdgeFeedbackCommand extends FeedbackCommand {
    static readonly KIND = UpdateCreateEdgeFeedbackAction.KIND;

    constructor(@inject(TYPES.Action) protected action: UpdateCreateEdgeFeedbackAction) {
        super();
    }

    /**
     * Updates feedback edge state and synchronizes node highlight state.
     *
     * @param context The command execution context
     * @returns The updated model root
     */
    execute(context: CommandExecutionContext): CommandReturn {
        const edge = context.root.index.getById(this.action.edgeId);
        if (!(edge instanceof GEdge) || !edge.edgeCreateData) {
            return context.root;
        }

        if (this.action.updatedSchema) {
            const parent = edge.parent;
            const idx = parent.children.indexOf(edge);
            parent.remove(edge);
            const newEdge = context.modelFactory.createElement(this.action.updatedSchema);
            if (newEdge instanceof GEdge) {
                newEdge.id = edge.id;
                newEdge.edgeCreateData = {
                    sourceId: newEdge.sourceId,
                    sourceAnchor: edge.edgeCreateData.sourceAnchor,
                    position: this.action.position,
                    anchor: this.action.anchor,
                    targetId: this.action.targetId
                };
                parent.add(newEdge, idx);
            }
        } else {
            edge.edgeCreateData = {
                sourceId: edge.edgeCreateData.sourceId,
                sourceAnchor: edge.edgeCreateData.sourceAnchor,
                position: this.action.position,
                anchor: this.action.anchor,
                targetId: this.action.targetId
            };
        }

        return context.root;
    }
}

/**
 * Command to stop create-edge feedback.
 * Removes the feedback edge from the model and clears any target highlight.
 */
@injectable()
export class StopCreateEdgeFeedbackCommand extends FeedbackCommand {
    static readonly KIND = StopCreateEdgeFeedbackAction.KIND;

    constructor(@inject(TYPES.Action) protected action: StopCreateEdgeFeedbackAction) {
        super();
    }

    /**
     * Removes the feedback edge and clears highlight on the previously hovered target.
     *
     * @param context The command execution context
     * @returns The updated model root
     */
    execute(context: CommandExecutionContext): CommandReturn {
        const edge = context.root.index.getById(this.action.edgeId);
        if (edge instanceof GEdge) {
            edge.parent.remove(edge);
        }
        return context.root;
    }
}

/**
 * Action to set or clear the edge-edit highlight on a single node.
 * Used during phase 1 of create-edge to preview the source anchor.
 */
export interface SetEdgeEditHighlightAction extends Action {
    kind: typeof SetEdgeEditHighlightAction.KIND;
    /**
     * The ID of the node to highlight.
     */
    nodeId: string | undefined;
    /**
     * The highlight to apply, or undefined to clear.
     */
    highlight: EdgeEditHighlight | undefined;
}

export namespace SetEdgeEditHighlightAction {
    export const KIND = "setEdgeEditHighlight";

    /**
     * Creates a SetEdgeEditHighlightAction.
     *
     * @param nodeId The ID of the node
     * @param highlight The highlight to apply, or undefined to clear
     * @returns The action
     */
    export function create(
        nodeId: string | undefined,
        highlight: EdgeEditHighlight | undefined
    ): SetEdgeEditHighlightAction {
        return { kind: KIND, nodeId, highlight };
    }
}

/**
 * Command to set or clear the edge-edit highlight on a node.
 */
@injectable()
export class SetEdgeEditHighlightCommand extends FeedbackCommand {
    static readonly KIND = SetEdgeEditHighlightAction.KIND;

    constructor(
        @inject(TYPES.Action) protected action: SetEdgeEditHighlightAction,
        @inject(EdgeEditHighlightState) protected highlightState: EdgeEditHighlightState
    ) {
        super();
    }

    /**
     * Updates the global edge-edit highlight state.
     *
     * @param context The command execution context
     * @returns The updated model root
     */
    execute(context: CommandExecutionContext): CommandReturn {
        this.highlightState.nodeId = this.action.nodeId;
        this.highlightState.highlight = this.action.highlight;
        return context.root;
    }
}
