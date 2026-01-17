import { sharedImport } from "../../sharedImport.js";
import { EdgeRouter } from "../edge-rourting/edgeRouter.js";
import { FeedbackEdgeRouteMovingMouseListener } from "./feedbackEdgeRouteMovingMouseListener.js";
import type { SelectionService } from "@eclipse-glsp/client";
import { EdgeEditListener } from "./edgeEditListener.js";

const { BaseEditTool, SelectionService: SelectionServiceKey } = sharedImport("@eclipse-glsp/client");
const { injectable, inject } = sharedImport("inversify");

/**
 * Tool for editing edge routes by dragging segments.
 * Similar to ChangeBoundsTool but for edge routing.
 */
@injectable()
export class EdgeEditTool extends BaseEditTool {
    static ID = "edge-edit-tool";

    @inject(EdgeRouter) readonly edgeRouter!: EdgeRouter;
    @inject(SelectionServiceKey) protected readonly selectionService!: SelectionService;

    protected feedbackMovingListener?: FeedbackEdgeRouteMovingMouseListener;
    protected edgeEditListener?: EdgeEditListener;

    get id(): string {
        return EdgeEditTool.ID;
    }

    enable(): void {
        this.edgeEditListener = new EdgeEditListener(this);
        this.feedbackMovingListener = new FeedbackEdgeRouteMovingMouseListener(this);

        this.toDisposeOnDisable.push(
            this.edgeEditListener,
            this.mouseTool.registerListener(this.edgeEditListener),
            this.feedbackMovingListener,
            this.selectionService.addListener(this.edgeEditListener)
        );
    }

    /**
     * Registers the feedback listener for mouse tracking.
     */
    registerFeedbackListeners(): void {
        if (this.feedbackMovingListener) {
            this.mouseTool.register(this.feedbackMovingListener);
        }
    }

    /**
     * Deregisters the feedback listener.
     */
    deregisterFeedbackListeners(): void {
        if (this.feedbackMovingListener) {
            this.feedbackMovingListener.dispose();
            this.mouseTool.deregister(this.feedbackMovingListener);
        }
    }

    /**
     * Gets the feedback moving listener.
     */
    getFeedbackMovingListener(): FeedbackEdgeRouteMovingMouseListener | undefined {
        return this.feedbackMovingListener;
    }
}
