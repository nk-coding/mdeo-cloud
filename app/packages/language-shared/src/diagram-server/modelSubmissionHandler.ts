import type { DirtyStateChangeReason, MaybePromise, Action } from "@eclipse-glsp/protocol";
import { sharedImport } from "../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { ModelSubmissionHandler: BaseModelSubmissionHandler } = sharedImport("@eclipse-glsp/server");

/**
 * Custom model submission handler that skips request bounds action when needsClientLayout is true because we handle this on the client side
 */
@injectable()
export class ModelSubmissionHandler extends BaseModelSubmissionHandler {
    override submitModel(reason?: DirtyStateChangeReason): MaybePromise<Action[]> {
        this.modelFactory.createModel();

        const revision = this.requestModelAction ? 0 : this.modelState.root.revision! + 1;
        this.modelState.root.revision = revision;

        return this.submitModelDirectly(reason);
    }
}
