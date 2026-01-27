import type { LayoutOperation } from "@mdeo/editor-protocol";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import type { Command } from "@eclipse-glsp/server";
import { BaseLayoutEngine } from "../layoutEngine.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Handler for layout operations.
 */
@injectable()
export class LayoutOperationHandler extends BaseOperationHandler {
    override readonly operationType = "layout" satisfies LayoutOperation["kind"];

    /**
     * The layout engine used to perform diagram layouting
     */
    @inject(BaseLayoutEngine)
    protected readonly layoutEngine!: BaseLayoutEngine;

    override async createCommand(operation: LayoutOperation): Promise<Command> {
        return new OperationHandlerCommand(this.modelState, undefined, await this.layoutEngine.layout(operation));
    }
}
