import { sharedImport } from "../../sharedImport.js";
import type { ModelState } from "../modelState.js";

const { injectable } = sharedImport("inversify");
const { OperationHandler } = sharedImport("@eclipse-glsp/server");

/**
 * Base class for operation handlers in the diagram server.
 * Provides common functionality for handling diagram operations.
 */
@injectable()
export abstract class BaseOperationHandler extends OperationHandler {
    declare modelState: ModelState;
}
