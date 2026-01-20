import { sharedImport } from "../../sharedImport.js";
import { EdgeRouter } from "../edge-rourting/edgeRouter.js";
import { FeedbackEdgeRouteMovingMouseListener } from "./feedbackEdgeRouteMovingMouseListener.js";
import { FeedbackEdgeReconnectMouseListener } from "./feedbackEdgeReconnectMouseListener.js";
import type { SelectionService } from "@eclipse-glsp/client";
import { EdgeEditListener } from "./edgeEditListener.js";
import { ElementFinder } from "../element-finder/elementFinder.js";

const { BaseEditTool, SelectionService: SelectionServiceKey } = sharedImport("@eclipse-glsp/client");
const { injectable, inject } = sharedImport("inversify");

/**
 * Tool for editing edge routes by dragging segments.
 * Similar to ChangeBoundsTool but for edge routing.
 */
@injectable()
export class EdgeEditTool extends BaseEditTool {
    static readonly ID = "edge-edit-tool";

    /**
     * Edge router for computing routes.
     */
    @inject(EdgeRouter) readonly edgeRouter!: EdgeRouter;
    /**
     * Element finder for locating elements from DOM coordinates.
     */
    @inject(ElementFinder) readonly elementFinder!: ElementFinder;
    /**
     * Selection service for managing selections.
     */
    @inject(SelectionServiceKey) protected readonly selectionService!: SelectionService;

    /**
     * Feedback listeners for edge editing.
     */
    protected feedbackMovingListener?: FeedbackEdgeRouteMovingMouseListener;
    /**
     * Feedback listener for edge reconnecting.
     */
    protected feedbackReconnectListener?: FeedbackEdgeReconnectMouseListener;
    /**
     * Listener for edge editing.
     */
    protected edgeEditListener?: EdgeEditListener;

    get id(): string {
        return EdgeEditTool.ID;
    }

    override enable(): void {
        this.edgeEditListener = new EdgeEditListener(this);
        this.feedbackMovingListener = new FeedbackEdgeRouteMovingMouseListener(this);
        this.feedbackReconnectListener = new FeedbackEdgeReconnectMouseListener(this);

        this.toDisposeOnDisable.push(
            this.edgeEditListener,
            this.feedbackMovingListener,
            this.feedbackReconnectListener,
            this.selectionService.addListener(this.edgeEditListener),
            this.mouseTool.registerListener(this.edgeEditListener)
        );
    }

    /**
     * Registers the feedback listener for mouse tracking.
     */
    registerFeedbackListeners(): void {
        if (this.feedbackMovingListener) {
            this.mouseTool.register(this.feedbackMovingListener);
        }
        if (this.feedbackReconnectListener) {
            this.mouseTool.register(this.feedbackReconnectListener);
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
        if (this.feedbackReconnectListener) {
            this.feedbackReconnectListener.dispose();
            this.mouseTool.deregister(this.feedbackReconnectListener);
        }
    }

    /**
     * Gets the feedback moving listener.
     *
     * @returns The feedback moving listener
     */
    getFeedbackMovingListener(): FeedbackEdgeRouteMovingMouseListener | undefined {
        return this.feedbackMovingListener;
    }

    /**
     * Gets the feedback reconnect listener.
     *
     * @returns The feedback reconnect listener
     */
    getFeedbackReconnectListener(): FeedbackEdgeReconnectMouseListener | undefined {
        return this.feedbackReconnectListener;
    }
}
