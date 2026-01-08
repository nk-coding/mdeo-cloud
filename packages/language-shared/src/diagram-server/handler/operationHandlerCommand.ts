import type { Command, MaybePromise } from "@eclipse-glsp/server";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");

@injectable()
export class OperationHandlerCommand implements Command {

    constructor(

    ) {}

    execute(): MaybePromise<void> {
        throw new Error("Method not implemented.");
    }

    undo(): MaybePromise<void> {
        throw new Error("Method not implemented.");
    }

    redo(): MaybePromise<void> {
        throw new Error("Method not implemented.");
    }

    canUndo(): boolean {
        return false;
    }

}