import type { AstReflection } from "@mdeo/language-common";
import { sharedImport } from "../../sharedImport.js";
import { AstReflectionKey } from "../langiumServices.js";
import type { GModelIndex } from "../modelIndex.js";
import type { ModelState } from "../modelState.js";

const { injectable, inject } = sharedImport("inversify");
const { OperationHandler, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Base class for operation handlers in the diagram server.
 * Provides common functionality for handling diagram operations.
 */
@injectable()
export abstract class BaseOperationHandler extends OperationHandler {
    declare modelState: ModelState;

    /**
     * Injected model index for accessing graph model elements.
     */
    @inject(GModelIndexKey)
    protected index!: GModelIndex;

    /**
     * Injected AST reflection service for type checking and model introspection.
     */
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;
}
