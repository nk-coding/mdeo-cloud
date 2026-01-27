import type { MaybePromise, ChangeBoundsOperation as ChangeBoundsOperationType } from "@eclipse-glsp/protocol";
import type { Command } from "@eclipse-glsp/server";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";
import type { NodeMetadata } from "../metadata.js";
import type { NodeLayoutMetadata } from "@mdeo/editor-protocol";

const { injectable } = sharedImport("inversify");
const { ChangeBoundsOperation } = sharedImport("@eclipse-glsp/protocol");

/**
 * Handler for change bounds operations.
 * Processes operations that change the position or size of diagram elements.
 */
@injectable()
export class ChangeBoundsOperationHandler extends BaseOperationHandler {
    override readonly operationType = ChangeBoundsOperation.KIND;

    /**
     * Creates a command to execute the change bounds operation.
     *
     * @param operation The change bounds operation to process
     * @returns The command to execute, or undefined if the operation cannot be handled
     */
    override createCommand(operation: ChangeBoundsOperationType): MaybePromise<Command | undefined> {
        return new OperationHandlerCommand(this.modelState, undefined, {
            nodes: Object.fromEntries(
                operation.newBounds.map(({ elementId, newSize, newPosition }) => [
                    elementId,
                    {
                        meta: {
                            position: newPosition,
                            prefWidth: newSize.width,
                            prefHeight: newSize.height
                        } satisfies NodeLayoutMetadata
                    } satisfies Partial<NodeMetadata>
                ])
            )
        });
    }
}
