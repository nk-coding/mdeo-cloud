import type { Operation, MaybePromise } from "@eclipse-glsp/protocol";
import type { Command } from "@eclipse-glsp/server";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { OperationHandler } = sharedImport("@eclipse-glsp/server");
const { ChangeBoundsOperation } = sharedImport("@eclipse-glsp/protocol");

/**
 * Handler for change bounds operations.
 * Processes operations that change the position or size of diagram elements.
 * @todo Implementation in progress
 */
@injectable()
export class ChangeBoundsOperationHandler extends OperationHandler {

    /** The operation type this handler processes */
    override readonly operationType = ChangeBoundsOperation.KIND;

    /**
     * Creates a command to execute the change bounds operation.
     * 
     * @param operation The change bounds operation to process
     * @returns The command to execute, or undefined if the operation cannot be handled
     * @todo Complete implementation
     */
    override createCommand(operation: Operation): MaybePromise<Command | undefined> {
        console.log("!!!!!!!!!!!!!!!!!!!!!")
        console.log(operation)
        throw new Error("Method not implemented.");
    }

}