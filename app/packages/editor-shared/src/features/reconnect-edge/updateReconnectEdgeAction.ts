import { sharedImport } from "../../sharedImport.js";
import type { GEdge } from "../../model/edge.js";
import type { Action, IActionDispatcher, CommandExecutionContext, CommandReturn } from "@eclipse-glsp/sprotty";
import type { EdgeLayoutMetadata } from "@mdeo/protocol-common";

const { injectable, inject } = sharedImport("inversify");
const { Command, TYPES } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Action to update edge metadata after a reconnect operation.
 * This action is dispatched after the server processes the reconnect
 * to update the client-side metadata.
 */
export interface UpdateReconnectEdgeAction extends Action {
    kind: "updateReconnectEdge";
    /**
     * The old ID of the edge before reconnection.
     */
    oldEdgeId: string;
    /**
     * The new ID of the edge after reconnection.
     * If undefined, metadata is not updated.
     */
    newEdgeId?: string;
    /**
     * The new metadata for the edge.
     * If undefined, metadata is not updated.
     */
    metadata?: EdgeLayoutMetadata;
}

export namespace UpdateReconnectEdgeAction {
    export const KIND = "updateReconnectEdge";

    /**
     * Creates a new UpdateReconnectEdgeAction.
     *
     * @param oldEdgeId The old edge ID
     * @param newEdgeId The new edge ID (optional if metadata is not updated)
     * @param metadata The new edge metadata (optional if not updated)
     * @returns The action
     */
    export function create(
        oldEdgeId: string,
        newEdgeId?: string,
        metadata?: EdgeLayoutMetadata
    ): UpdateReconnectEdgeAction {
        return {
            kind: KIND,
            oldEdgeId,
            newEdgeId,
            metadata
        };
    }
}

/**
 * Command that updates edge metadata after reconnection.
 * Handles the case where the edge ID may have changed.
 */
@injectable()
export class UpdateReconnectEdgeCommand extends Command {
    static readonly KIND = UpdateReconnectEdgeAction.KIND;

    @inject(TYPES.IActionDispatcher) protected actionDispatcher!: IActionDispatcher;

    constructor(@inject(TYPES.Action) protected readonly action: UpdateReconnectEdgeAction) {
        super();
    }

    override execute(context: CommandExecutionContext): CommandReturn {
        return this.updateEdgeMetadata(context);
    }

    override undo(): CommandReturn {
        throw new Error("Method not implemented.");
    }

    override redo(): CommandReturn {
        throw new Error("Method not implemented.");
    }

    /**
     * Updates edge metadata in the model state.
     * If the edge ID changed, transfers metadata from old to new ID.
     * Only updates if newEdgeId and metadata are provided.
     *
     * @param context The command execution context
     * @returns The updated root
     */
    protected updateEdgeMetadata(context: CommandExecutionContext): CommandReturn {
        if (this.action.newEdgeId != undefined && this.action.metadata != undefined) {
            const edge = context.root.index.getById(this.action.newEdgeId) as GEdge | undefined;

            if (edge) {
                edge.meta = this.action.metadata;
            }
        }

        return context.root;
    }
}
