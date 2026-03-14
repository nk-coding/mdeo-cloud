import type { Action, MaybePromise, ActionHandler } from "@eclipse-glsp/server";
import { sharedImport } from "../../sharedImport.js";
import {
    RequestCreateEdgeInitialSchemaAction,
    RequestCreateEdgeTargetSchemaAction,
    SetCreateEdgeInitialSchemaAction,
    SetCreateEdgeTargetSchemaAction
} from "@mdeo/protocol-common";
import { CreateEdgeSchemaResolver } from "../createEdgeSchemaResolver.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Handles create-edge schema request actions and returns corresponding response actions.
 */
@injectable()
export class RequestCreateEdgeSchemaActionHandler implements ActionHandler {
    @inject(CreateEdgeSchemaResolver)
    protected readonly resolver!: CreateEdgeSchemaResolver;

    readonly actionKinds: string[] = [
        RequestCreateEdgeInitialSchemaAction.KIND,
        RequestCreateEdgeTargetSchemaAction.KIND
    ];

    /**
     * Entrypoint for handling create-edge schema request actions.
     *
     * @param action The incoming action
     * @returns A promise or value containing response actions
     */
    execute(action: Action): MaybePromise<Action[]> {
        return this.executeInternal(action);
    }

    /**
     * Resolves schema request actions and maps them to response actions.
     *
     * @param action The incoming action
     * @returns A list containing the corresponding response action, or an empty list
     */
    protected async executeInternal(action: Action): Promise<Action[]> {
        if (RequestCreateEdgeInitialSchemaAction.is(action)) {
            const schema = await this.resolver.getInitialSchema({
                sourceElementId: action.sourceElementId,
                context: action.context
            });
            return [SetCreateEdgeInitialSchemaAction.create({ schema, responseId: action.requestId })];
        }

        if (RequestCreateEdgeTargetSchemaAction.is(action)) {
            const schema = await this.resolver.getTargetSchema({
                sourceElementId: action.sourceElementId,
                targetElementId: action.targetElementId,
                context: action.context
            });
            return [SetCreateEdgeTargetSchemaAction.create({ schema, responseId: action.requestId })];
        }

        return [];
    }
}
