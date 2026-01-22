import type { MaybePromise } from "@eclipse-glsp/protocol";
import type { Command } from "@eclipse-glsp/server";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import type { TriggerActionOperation } from "@mdeo/editor-protocol";
import { createTriggerActionProtocol, type ActionStartParams } from "@mdeo/language-common";

const { injectable } = sharedImport("inversify");
const vscodeJsonrpc = sharedImport("vscode-jsonrpc");

/**
 * Handler for trigger action operations from the GLSP client.
 * Forwards action trigger requests to the workbench client via LSP notifications.
 */
@injectable()
export class TriggerActionOperationHandler extends BaseOperationHandler {
    /**
     * The operation type this handler processes
     */
    override readonly operationType: TriggerActionOperation["kind"] = "triggerAction";

    /**
     * Creates a command to execute the trigger action operation.
     * Instead of returning a command that modifies the model, this handler
     * sends a notification directly to the workbench to trigger the action dialog.
     *
     * @param operation The trigger action operation to process
     * @returns undefined since no model changes are made
     */
    override createCommand(operation: TriggerActionOperation): MaybePromise<Command | undefined> {
        const TriggerActionProtocol = createTriggerActionProtocol(vscodeJsonrpc);
        const connection = this.modelState.languageServices.shared.lsp.Connection;

        if (connection == undefined) {
            // eslint-disable-next-line no-console
            console.warn(
                "[TriggerActionOperationHandler] Cannot send trigger action notification: LSP connection is unavailable"
            );
            return undefined;
        }

        const actionParams: ActionStartParams = {
            type: operation.actionType,
            languageId: operation.languageId,
            data: operation.data
        };
        connection.sendNotification(TriggerActionProtocol.TriggerActionNotification, actionParams);

        return undefined;
    }
}
