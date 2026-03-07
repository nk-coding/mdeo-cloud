import type { MaybePromise, Operation } from "@eclipse-glsp/protocol";
import type { Command } from "@eclipse-glsp/server";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";

const { injectable } = sharedImport("inversify");

/**
 * Handler for update client operations.
 * Processes operations that trigger client updates without modifying the model.
 */
@injectable()
export class UpdateClientOperationHandler extends BaseOperationHandler {
    /**
     * The operation type this handler processes
     */
    override readonly operationType = UpdateClientOperation.KIND;

    override createCommand(): MaybePromise<Command | undefined> {
        return new OperationHandlerCommand(this.modelState, undefined, {});
    }
}

/**
 * Operation to trigger a client update.
 */
export interface UpdateClientOperation extends Operation {
    kind: typeof UpdateClientOperation.KIND;
}

export namespace UpdateClientOperation {
    export const KIND = "updateClientOperation";

    export function create(): UpdateClientOperation {
        return {
            kind: KIND,
            isOperation: true
        };
    }
}
