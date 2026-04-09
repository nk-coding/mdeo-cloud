import type { Command, MaybePromise, ActionDispatcher } from "@eclipse-glsp/server";
import type { CutOperation } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";

const { injectable, inject } = sharedImport("inversify");
const { CutOperation: CutOperationKind, DeleteElementOperation } = sharedImport("@eclipse-glsp/protocol");
const { ActionDispatcher: ActionDispatcherKey } = sharedImport("@eclipse-glsp/server");

/**
 * Operation handler for cut operations.
 *
 * <p>A cut is effectively a copy followed by a delete. The copy is already
 * handled client-side before the {@link CutOperation} is dispatched to the
 * server. This handler only needs to delete the selected elements by
 * dispatching a {@link DeleteElementOperation}.
 */
@injectable()
export class BaseCutOperationHandler extends BaseOperationHandler {
    readonly operationType = CutOperationKind.KIND;

    /**
     * The action dispatcher used to dispatch the delete operation.
     */
    @inject(ActionDispatcherKey)
    protected actionDispatcher!: ActionDispatcher;

    /**
     * Creates a command for the cut operation by dispatching a delete
     * operation for the selected elements.
     *
     * @param operation The cut operation containing the editor context with selected element IDs.
     * @returns Always {@code undefined} since the delete operation handles the actual model change.
     */
    createCommand(operation: CutOperation): MaybePromise<Command | undefined> {
        const selectedIds = operation.editorContext.selectedElementIds;
        if (selectedIds.length > 0) {
            this.actionDispatcher.dispatch(DeleteElementOperation.create(selectedIds));
        }
        return undefined;
    }
}
