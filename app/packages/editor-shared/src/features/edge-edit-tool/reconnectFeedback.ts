import type { Action, CommandExecutionContext, CommandReturn } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import type { Point } from "@eclipse-glsp/protocol";
import type { EdgeAnchor } from "@mdeo/editor-protocol";
import { GEdge, type ReconnectEnd } from "../../model/edge.js";
import { GNode } from "../../model/node.js";

const { injectable, inject } = sharedImport("inversify");
const { TYPES } = sharedImport("@eclipse-glsp/sprotty");
const { FeedbackCommand } = sharedImport("@eclipse-glsp/client");

/**
 * Action to start edge reconnect mode.
 */
export interface StartEdgeReconnectFeedbackAction extends Action {
    kind: typeof StartEdgeReconnectFeedbackAction.KIND;
    /**
     * The ID of the edge being reconnected.
     */
    edgeId: string;
    /**
     * Which end is being reconnected.
     */
    end: ReconnectEnd;
    /**
     * The initial position of the reconnecting end.
     */
    position: Point;
}

export namespace StartEdgeReconnectFeedbackAction {
    export const KIND = "startEdgeReconnectFeedback";

    /**
     * Creates a new StartEdgeReconnectFeedbackAction.
     *
     * @param edgeId The ID of the edge being reconnected
     * @param end Which end is being reconnected
     * @param position The initial position
     * @returns The action
     */
    export function create(edgeId: string, end: ReconnectEnd, position: Point): StartEdgeReconnectFeedbackAction {
        return { kind: KIND, edgeId, end, position };
    }
}

/**
 * Action to update the reconnect position.
 */
export interface UpdateEdgeReconnectFeedbackAction extends Action {
    kind: typeof UpdateEdgeReconnectFeedbackAction.KIND;
    /**
     * The ID of the edge being reconnected.
     */
    edgeId: string;
    /**
     * The new free position of the reconnecting end (when not snapped).
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
}

export namespace UpdateEdgeReconnectFeedbackAction {
    export const KIND = "updateEdgeReconnectFeedback";

    /**
     * Creates a new UpdateEdgeReconnectFeedbackAction.
     *
     * @param edgeId The ID of the edge being reconnected
     * @param position The new free position (when not snapped)
     * @param anchor The anchor (when snapped to target)
     * @param targetId The target element ID if hovering over a valid target
     * @returns The action
     */
    export function create(
        edgeId: string,
        position?: Point,
        anchor?: EdgeAnchor,
        targetId?: string
    ): UpdateEdgeReconnectFeedbackAction {
        return { kind: KIND, edgeId, position, anchor, targetId };
    }
}

/**
 * Action to stop edge reconnect mode.
 */
export interface StopEdgeReconnectFeedbackAction extends Action {
    kind: typeof StopEdgeReconnectFeedbackAction.KIND;
    /**
     * The ID of the edge to stop reconnecting.
     */
    edgeId: string;
}

export namespace StopEdgeReconnectFeedbackAction {
    export const KIND = "stopEdgeReconnectFeedback";

    /**
     * Creates a new StopEdgeReconnectFeedbackAction.
     *
     * @param edgeId The ID of the edge to stop reconnecting
     * @returns The action
     */
    export function create(edgeId: string): StopEdgeReconnectFeedbackAction {
        return { kind: KIND, edgeId };
    }
}

/**
 * Command to start edge reconnect feedback.
 */
@injectable()
export class StartEdgeReconnectFeedbackCommand extends FeedbackCommand {
    static readonly KIND = StartEdgeReconnectFeedbackAction.KIND;

    constructor(@inject(TYPES.Action) protected action: StartEdgeReconnectFeedbackAction) {
        super();
    }

    execute(context: CommandExecutionContext): CommandReturn {
        const edge = context.root.index.getById(this.action.edgeId);
        if (edge instanceof GEdge) {
            edge.reconnectData = {
                end: this.action.end,
                position: this.action.position
            };
        }
        return context.root;
    }
}

/**
 * Command to update edge reconnect feedback.
 */
@injectable()
export class UpdateEdgeReconnectFeedbackCommand extends FeedbackCommand {
    static readonly KIND = UpdateEdgeReconnectFeedbackAction.KIND;

    constructor(@inject(TYPES.Action) protected action: UpdateEdgeReconnectFeedbackAction) {
        super();
    }

    execute(context: CommandExecutionContext): CommandReturn {
        const edge = context.root.index.getById(this.action.edgeId);
        if (edge instanceof GEdge && edge.reconnectData) {
            if (edge.reconnectData.targetId) {
                const prevTarget = context.root.index.getById(edge.reconnectData.targetId);
                if (prevTarget instanceof GNode) {
                    prevTarget.edgeEditHighlight = undefined;
                }
            }
            edge.reconnectData = {
                position: this.action.position,
                anchor: this.action.anchor,
                targetId: this.action.targetId,
                end: edge.reconnectData.end
            };

            if (this.action.targetId) {
                const newTarget = context.root.index.getById(this.action.targetId);
                if (newTarget instanceof GNode) {
                    newTarget.edgeEditHighlight = { type: "reconnect" };
                }
            }
        }
        return context.root;
    }
}

/**
 * Command to stop edge reconnect feedback.
 */
@injectable()
export class StopEdgeReconnectFeedbackCommand extends FeedbackCommand {
    static readonly KIND = StopEdgeReconnectFeedbackAction.KIND;

    constructor(@inject(TYPES.Action) protected action: StopEdgeReconnectFeedbackAction) {
        super();
    }

    execute(context: CommandExecutionContext): CommandReturn {
        const edge = context.root.index.getById(this.action.edgeId);
        if (edge instanceof GEdge && edge.reconnectData) {
            if (edge.reconnectData.targetId) {
                const target = context.root.index.getById(edge.reconnectData.targetId);
                if (target instanceof GNode) {
                    target.edgeEditHighlight = undefined;
                }
            }
            delete edge.reconnectData;
        }
        return context.root;
    }
}
