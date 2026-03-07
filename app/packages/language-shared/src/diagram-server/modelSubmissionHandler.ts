import type { DirtyStateChangeReason, Action } from "@eclipse-glsp/protocol";
import { sharedImport } from "../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { ModelSubmissionHandler: BaseModelSubmissionHandler } = sharedImport("@eclipse-glsp/server");
const { UpdateModelAction } = sharedImport("@eclipse-glsp/protocol");

/**
 * Custom model submission handler that skips request bounds action when needsClientLayout is true because we handle this on the client side
 */
@injectable()
export class ModelSubmissionHandler extends BaseModelSubmissionHandler {
    /**
     * The last time a model submission was made
     */
    private lastSubmissionTime = 0;

    override async submitModel(reason?: DirtyStateChangeReason): Promise<Action[]> {
        this.modelFactory.createModel();

        const revision = this.requestModelAction ? 0 : this.modelState.root.revision! + 1;
        this.modelState.root.revision = revision;
        const now = Date.now();
        const doAnimation = now - this.lastSubmissionTime > 250;
        this.lastSubmissionTime = now;

        const result = await this.submitModelDirectly(reason);
        for (const action of result) {
            if (UpdateModelAction.is(action)) {
                action.animate = doAnimation;
            }
        }
        return result;
    }
}
