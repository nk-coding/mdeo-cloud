import type { ResetLayoutOperation } from "@mdeo/protocol-common";
import type { Command } from "@eclipse-glsp/server";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";

const { injectable } = sharedImport("inversify");

/**
 * Server-side handler for {@link ResetLayoutOperation}.
 *
 * Clears the manually persisted sizing and / or routing information for a
 * diagram element so that the next model build computes fresh auto-layout
 * values for it.
 */
@injectable()
export class ResetLayoutOperationHandler extends BaseOperationHandler {
    override readonly operationType = "resetLayout" satisfies ResetLayoutOperation["kind"];

    override createCommand(operation: ResetLayoutOperation): Command {
        const elementId = operation.elementId;
        const scope = operation.scope ?? "all";
        const modelState = this.modelState;

        const newMetadata: Record<string, undefined> = {};
        if (scope === "bounds" || scope === "all") {
            newMetadata["prefWidth"] = undefined;
            newMetadata["prefHeight"] = undefined;
        }
        if (scope === "routing" || scope === "all") {
            newMetadata["routingPoints"] = undefined;
            newMetadata["sourceAnchor"] = undefined;
            newMetadata["targetAnchor"] = undefined;
        }

        return new OperationHandlerCommand(modelState, undefined, {
            nodes: {
                [elementId]: {
                    meta: newMetadata
                }
            }
        });
    }
}
