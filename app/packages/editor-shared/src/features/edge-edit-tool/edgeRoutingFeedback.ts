import type { Action, CommandExecutionContext, CommandReturn, Point } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import type { GEdge } from "../../model/edge.js";
import type { EdgeLayoutMetadata } from "@mdeo/protocol-common";

const { injectable, inject } = sharedImport("inversify");
const { FeedbackCommand, TYPES } = sharedImport("@eclipse-glsp/client");

/**
 * Action to set edge routing feedback during editing.
 * Updates the routing points and anchors of an edge for visual feedback.
 */
export interface SetEdgeRoutingFeedbackAction extends Action {
    kind: typeof SetEdgeRoutingFeedbackAction.KIND;
    /**
     * The ID of the edge to update.
     */
    elementId: string;
    /**
     * The new routing points.
     */
    routingPoints?: Point[];
    /**
     * The new start anchor.
     */
    sourceAnchor?: EdgeLayoutMetadata["sourceAnchor"];
    /**
     * The new target anchor.
     */
    targetAnchor?: EdgeLayoutMetadata["targetAnchor"];
}

export namespace SetEdgeRoutingFeedbackAction {
    export const KIND = "setEdgeRoutingFeedback";

    /**
     * Type guard for SetEdgeRoutingFeedbackAction.
     */
    export function is(object: unknown): object is SetEdgeRoutingFeedbackAction {
        return typeof object === "object" && object !== null && (object as SetEdgeRoutingFeedbackAction).kind === KIND;
    }

    /**
     * Creates a SetEdgeRoutingFeedbackAction.
     *
     * @param options The action options
     * @returns The created action
     */
    export function create(options: {
        elementId: string;
        routingPoints?: Point[];
        sourceAnchor?: EdgeLayoutMetadata["sourceAnchor"];
        targetAnchor?: EdgeLayoutMetadata["targetAnchor"];
    }): SetEdgeRoutingFeedbackAction {
        return {
            kind: KIND,
            ...options
        };
    }
}

/**
 * Command to apply edge routing feedback.
 */
@injectable()
export class SetEdgeRoutingFeedbackCommand extends FeedbackCommand {
    static readonly KIND = SetEdgeRoutingFeedbackAction.KIND;

    constructor(@inject(TYPES.Action) protected action: SetEdgeRoutingFeedbackAction) {
        super();
    }

    execute(context: CommandExecutionContext): CommandReturn {
        const edge = context.root.index.getById(this.action.elementId);
        if (edge && "meta" in edge) {
            const gEdge = edge as GEdge;

            const newMeta: EdgeLayoutMetadata = {
                routingPoints: this.action.routingPoints ?? gEdge.meta?.routingPoints ?? [],
                sourceAnchor: this.action.sourceAnchor ?? gEdge.meta?.sourceAnchor,
                targetAnchor: this.action.targetAnchor ?? gEdge.meta?.targetAnchor
            };

            gEdge.meta = newMeta;
        }

        return context.root;
    }
}
