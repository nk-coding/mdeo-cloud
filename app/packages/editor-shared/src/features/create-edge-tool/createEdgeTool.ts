import type { Action, GModelElement, IActionDispatcher, IModelFactory } from "@eclipse-glsp/sprotty";
import type { Point } from "@eclipse-glsp/protocol";
import type { EdgeAnchor, CreateEdgeOperation, CreateEdgeSchema } from "@mdeo/editor-protocol";
import { sharedImport } from "../../sharedImport.js";
import { EdgeRouter } from "../edge-routing/edgeRouter.js";
import { ElementFinder } from "../element-finder/elementFinder.js";
import { GNode } from "../../model/node.js";
import { GEdge } from "../../model/edge.js";
import { isConnectable } from "../edge-routing/connectable.js";
import { getRelativePosition } from "../../base/getRelativePosition.js";
import {
    SetEdgeEditHighlightAction,
    StartCreateEdgeFeedbackAction,
    StopCreateEdgeFeedbackAction,
    UpdateCreateEdgeFeedbackAction
} from "./createEdgeFeedback.js";
import { CreateEdgeProvider } from "./createEdgeProvider.js";

const { injectable, inject } = sharedImport("inversify");
const {
    BaseEditTool,
    DragAwareMouseListener,
    EnableDefaultToolsAction,
    cursorFeedbackAction,
    findParentByFeature,
    TYPES,
    ModifyCSSFeedbackAction
} = sharedImport("@eclipse-glsp/client");
const { GChildElement } = sharedImport("@eclipse-glsp/sprotty");

/**
 * CSS class applied while the create-edge tool is active (crosshair cursor).
 */
const CSS_CREATE_EDGE = "edge-creation-mode";

/**
 * Unique feedback edge ID used during the creation flow.
 */
const FEEDBACK_EDGE_ID = "__create-edge-feedback";
const CLICK_MAX_DURATION_MS = 180;
const DRAG_MIN_DISTANCE_PX = 5;

/**
 * Tracks the state of an in-flight async schema validation request.
 */
interface SchemaRequest {
    /**
     * Monotonic token used to detect and discard stale responses.
     */
    requestToken: number;
    /**
     * The element ID that was last submitted for validation, if any.
     */
    schemaId?: string;
}

/**
 * On-demand tool for creating edges between nodes.
 * Implements a two-phase flow:
 * 1. Source selection: crosshair cursor, hover highlights valid source nodes with anchor cue.
 * 2. Target selection: feedback edge visible, hover highlights valid target nodes,
 *    click/release on valid target dispatches {@link CreateEdgeOperation}.
 *
 * Requires a bound {@link CreateEdgeProvider} in the DI container.
 */
@injectable()
export class CreateEdgeTool extends BaseEditTool {
    static readonly ID = "mdeo.create-edge-tool";

    @inject(EdgeRouter) readonly edgeRouter!: EdgeRouter;
    @inject(ElementFinder) readonly elementFinder!: ElementFinder;
    @inject(CreateEdgeProvider) readonly provider!: CreateEdgeProvider;
    @inject(TYPES.IModelFactory) readonly modelFactory!: IModelFactory;

    get id(): string {
        return CreateEdgeTool.ID;
    }

    override enable(): void {
        const listener = new CreateEdgeMouseListener(this, this.actionDispatcher);

        this.toDisposeOnDisable.push(
            listener,
            this.mouseTool.registerListener(listener),
            this.createFeedbackEmitter()
                .add(
                    cursorFeedbackAction(CSS_CREATE_EDGE),
                    ModifyCSSFeedbackAction.create({ add: [], remove: [CSS_CREATE_EDGE] })
                )
                .submit()
        );
    }

    /**
     * Creates a typed edge model from the current create-edge schema template.
     *
     * @param schema The schema containing the feedback edge template
     * @returns The created edge model, or undefined if the schema does not produce a GEdge
     */
    createEdgeFromSchema(schema: CreateEdgeSchema | undefined): GEdge | undefined {
        if (schema == undefined) {
            return undefined;
        }

        const edge = this.modelFactory.createElement(schema.template);
        return edge instanceof GEdge ? edge : undefined;
    }
}

/**
 * Mouse listener that drives the two-phase edge creation flow.
 *
 * Phase 1 (source selection):
 * - Hovering over a connectable node sets an edge-edit highlight with an anchor cue.
 * - Clicking a valid source node transitions to phase 2.
 *
 * Phase 2 (target selection):
 * - A feedback edge is rendered from the source anchor to the cursor.
 * - Hovering over a valid target node snaps the feedback edge to its nearest anchor
 *   and optionally updates the schema via the provider.
 * - Clicking a valid target dispatches the {@link CreateEdgeOperation}.
 * - Clicking empty space or an invalid target cancels creation.
 */
class CreateEdgeMouseListener extends DragAwareMouseListener {
    /**
     * The selected source node (set when entering phase 2).
     */
    private source?: GNode;
    /**
     * The active edge schema from the provider.
     */
    private schema?: CreateEdgeSchema;
    /**
     * The initial schema resolved for the source node without a target node
     */
    private initialSchema?: CreateEdgeSchema;
    /**
     * Currently highlighted node ID (source in phase 1, target in phase 2).
     * Only one can be highlighted at a time.
     */
    private highlightedNodeId?: string;
    /**
     * Tracks the last async source-validation request (used in phase 1 hover and creation-start).
     */
    private sourceRequest: SchemaRequest = { requestToken: 0 };
    /**
     * Tracks the last async target-validation request (used in phase 2 hover).
     */
    private targetRequest: SchemaRequest = { requestToken: 0 };
    /**
     * Source node candidate captured at mouse-down (before async resolves).
     */
    private mouseDownSource?: GNode;
    /**
     * Mouse-down timestamp for click-vs-drag differentiation.
     */
    private mouseDownAt?: number;
    /**
     * Graph position of the initial mouse-down.
     */
    private mouseDownGraphPosition?: Point;
    /**
     * Whether mouse movement exceeded the drag threshold since last mouse-down.
     */
    private mouseDownMoved = false;
    /**
     * Whether source was already active (phase 2) at the time of the last mouse-down.
     */
    private sourceWasActiveAtMouseDown = false;

    constructor(
        protected readonly tool: CreateEdgeTool,
        protected readonly actionDispatcher: IActionDispatcher
    ) {
        super();
    }

    override mouseMove(target: GModelElement, event: MouseEvent): Action[] {
        if ((event.buttons & 1) !== 0 && this.mouseDownGraphPosition) {
            const referenceNode = this.source ?? this.mouseDownSource;
            if (referenceNode) {
                const position = this.getRelativePositionForNode(referenceNode, event);
                const dx = position.x - this.mouseDownGraphPosition.x;
                const dy = position.y - this.mouseDownGraphPosition.y;
                if (Math.sqrt(dx * dx + dy * dy) >= DRAG_MIN_DISTANCE_PX) {
                    this.mouseDownMoved = true;
                }
            }
        }

        if (this.source) {
            return this.handlePhase2Move(target, event);
        }

        return this.handlePhase1Move(target, event);
    }

    override mouseDown(target: GModelElement, event: MouseEvent): Action[] {
        if (event.button !== 0) {
            this.clearMouseDownTracking();
            return [];
        }

        this.sourceWasActiveAtMouseDown = this.source != undefined;

        if (this.source != undefined) {
            this.mouseDownGraphPosition = this.getRelativePositionForNode(this.source, event);
            return [];
        }

        this.mouseDownAt = Date.now();
        this.mouseDownMoved = false;

        const actualTarget = this.resolveActualTarget(target, event);
        const node = this.findConnectableNode(actualTarget, "source");
        if (node == undefined) {
            this.clearMouseDownTracking();
            return [];
        }

        this.mouseDownSource = node;
        const sourcePosition = this.getRelativePositionForNode(node, event);
        this.mouseDownGraphPosition = sourcePosition;
        return this.startCreationMode(node, sourcePosition);
    }

    override mouseUp(target: GModelElement, event: MouseEvent): Action[] {
        if (event.button !== 0) {
            return [];
        }

        const mouseDownAt = this.mouseDownAt;
        const wasDragged = this.mouseDownMoved;
        this.clearMouseDownTracking();

        if (this.source == undefined) {
            this.sourceRequest.requestToken++;
            return [];
        }

        if (!this.sourceWasActiveAtMouseDown && !wasDragged && mouseDownAt != undefined) {
            const duration = Date.now() - mouseDownAt;
            if (duration < CLICK_MAX_DURATION_MS) {
                return [];
            }
        }

        return this.handlePhase2Click(target, event);
    }

    /**
     * Handles mouse movement in phase 1 by asynchronously validating and highlighting source nodes.
     * The highlight is only shown after {@link CreateEdgeProvider.getInitialSchema} confirms the
     * node can act as a valid edge source, preventing misleading highlights for structurally
     * connectable nodes that have no valid edge type to offer.
     *
     * @param target The current model target from sprotty
     * @param event The DOM mouse event
     * @returns Feedback actions for source highlight updates
     */
    private handlePhase1Move(target: GModelElement, event: MouseEvent): Action[] {
        const actions: Action[] = [];
        const actualTarget = this.resolveActualTarget(target, event);
        const node = this.findConnectableNode(actualTarget, "source");

        if (this.highlightedNodeId && this.highlightedNodeId !== node?.id) {
            actions.push(SetEdgeEditHighlightAction.create(undefined, undefined));
            this.highlightedNodeId = undefined;
        }

        if (node == undefined || this.mouseDownSource != undefined) {
            return actions;
        }

        if (this.sourceRequest.schemaId === node.id) {
            if (this.initialSchema != undefined) {
                const position = this.getRelativePositionForNode(node, event);
                const anchor = this.computeNearestAnchor(position, node.bounds);
                actions.push(
                    SetEdgeEditHighlightAction.create(node.id, {
                        type: "create",
                        anchorPosition: anchor
                    })
                );
            }
            return actions;
        }

        const requestToken = ++this.sourceRequest.requestToken;
        this.sourceRequest.schemaId = node.id;
        const position = this.getRelativePositionForNode(node, event);
        const anchor = this.computeNearestAnchor(position, node.bounds);

        this.tool.provider.getInitialSchema(node).then((schema) => {
            if (requestToken !== this.sourceRequest.requestToken) {
                return;
            }
            const edgeCandidate = schema ? this.tool.createEdgeFromSchema(schema) : undefined;
            if (!edgeCandidate || !node.canConnect(edgeCandidate, "source")) {
                this.initialSchema = undefined;
                if (this.highlightedNodeId === node.id) {
                    this.tool.dispatchActions([SetEdgeEditHighlightAction.create(undefined, undefined)]);
                    this.highlightedNodeId = undefined;
                }
                return;
            }
            this.initialSchema = schema;
            this.tool.dispatchActions([
                SetEdgeEditHighlightAction.create(node.id, {
                    type: "create",
                    anchorPosition: anchor
                })
            ]);
            this.highlightedNodeId = node.id;
        });

        return actions;
    }

    /**
     * Transitions to phase 2 using the already-fetched schema from phase-1 hover.
     * Returns an empty array if no schema has been loaded for the given source yet.
     *
     * @param source The selected source node
     * @param sourcePosition The fixed graph position where phase 2 starts
     * @returns Actions to start the feedback edge, or an empty array if the schema is not ready
     */
    private startCreationMode(source: GNode, sourcePosition: Point): Action[] {
        if (this.initialSchema == undefined || this.sourceRequest.schemaId !== source.id) {
            return [];
        }

        const schema = this.initialSchema;
        const edgeCandidate = this.tool.createEdgeFromSchema(schema);
        if (!edgeCandidate || !source.canConnect(edgeCandidate, "source")) {
            return [];
        }

        const sourceAnchor = this.tool.edgeRouter.projectAnchor(edgeCandidate, sourcePosition, source.bounds).anchor;

        this.sourceRequest.requestToken++;
        this.source = source;
        this.schema = schema;
        this.targetRequest = { requestToken: this.targetRequest.requestToken };

        const actions: Action[] = [];
        if (this.highlightedNodeId) {
            actions.push(SetEdgeEditHighlightAction.create(undefined, undefined));
            this.highlightedNodeId = undefined;
        }
        actions.push(
            StartCreateEdgeFeedbackAction.create(FEEDBACK_EDGE_ID, schema.template, sourcePosition, sourceAnchor)
        );
        return actions;
    }

    /**
     * Handles mouse movement in phase 2 by updating the feedback edge route
     * and optionally applying a target-specific schema.
     *
     * @param target The current model target from sprotty
     * @param event The DOM mouse event
     * @returns Feedback actions for edge route and highlight updates
     */
    private handlePhase2Move(target: GModelElement, event: MouseEvent): Action[] {
        const actualTarget = this.resolveActualTarget(target, event);
        const candidateEdge = this.getFeedbackEdge(target) ?? this.tool.createEdgeFromSchema(this.schema);
        const node = this.findConnectableNode(actualTarget, "target", candidateEdge);

        if (node != undefined && candidateEdge && this.source && this.schema && node.id !== this.source.id) {
            const position = this.getRelativePositionForNode(node, event);

            const anchor = this.tool.edgeRouter.projectAnchor(candidateEdge, position, node.bounds).anchor;

            const highlightActions: Action[] = [];
            if (this.highlightedNodeId !== node.id) {
                highlightActions.push(SetEdgeEditHighlightAction.create(node.id, { type: "create" }));
                this.highlightedNodeId = node.id;
            }

            if (this.targetRequest.schemaId !== node.id) {
                const requestToken = ++this.targetRequest.requestToken;
                const source = this.source;
                this.targetRequest.schemaId = node.id;

                this.tool.provider.getTargetSchema(source, node).then((updatedSchema) => {
                    if (requestToken !== this.targetRequest.requestToken || this.source == undefined) {
                        return;
                    }
                    if (updatedSchema == undefined) {
                        this.schema = this.initialSchema;
                        this.actionDispatcher.dispatch(
                            UpdateCreateEdgeFeedbackAction.create(
                                FEEDBACK_EDGE_ID,
                                position,
                                undefined,
                                undefined,
                                this.schema?.template
                            )
                        );
                        return;
                    }
                    this.schema = updatedSchema;
                    this.actionDispatcher.dispatch(
                        UpdateCreateEdgeFeedbackAction.create(
                            FEEDBACK_EDGE_ID,
                            undefined,
                            anchor,
                            node.id,
                            updatedSchema.template
                        )
                    );
                });
                return highlightActions;
            } else {
                if (this.schema === this.initialSchema) {
                    return [
                        ...highlightActions,
                        UpdateCreateEdgeFeedbackAction.create(FEEDBACK_EDGE_ID, position, undefined, undefined)
                    ];
                } else {
                    return [
                        ...highlightActions,
                        UpdateCreateEdgeFeedbackAction.create(FEEDBACK_EDGE_ID, undefined, anchor, node.id)
                    ];
                }
            }
        }

        this.schema = this.initialSchema;
        const relativeTarget = this.getPositionReference(target);
        const position = getRelativePosition(relativeTarget, event);
        const action = UpdateCreateEdgeFeedbackAction.create(
            FEEDBACK_EDGE_ID,
            position,
            undefined,
            undefined,
            this.targetRequest.schemaId != undefined ? this.schema?.template : undefined
        );
        this.targetRequest.schemaId = undefined;
        const result: Action[] = [action];
        if (this.highlightedNodeId != undefined) {
            result.push(SetEdgeEditHighlightAction.create(undefined, undefined));
            this.highlightedNodeId = undefined;
        }
        return result;
    }

    /**
     * Handles target selection click in phase 2 and dispatches create-edge operation.
     *
     * @param target The clicked model target
     * @param event The DOM mouse event
     * @returns Actions to finalize creation or cancel the flow
     */
    private handlePhase2Click(target: GModelElement, event: MouseEvent): Action[] {
        if (this.source == undefined || this.schema == undefined) {
            return this.cancelCreation();
        }

        const actualTarget = this.resolveActualTarget(target, event);
        const candidateEdge = this.getFeedbackEdge(target) ?? this.tool.createEdgeFromSchema(this.schema);
        const node = this.findConnectableNode(actualTarget, "target", candidateEdge);

        if (node == undefined || node.id === this.source.id) {
            return this.cancelCreation();
        }

        const feedbackEdge = this.getFeedbackEdge(target);
        let routingPoints: Point[] = [];
        let sourceAnchor: EdgeAnchor | undefined;
        let targetAnchor: EdgeAnchor | undefined;

        if (feedbackEdge) {
            const route = this.tool.edgeRouter.computeRoute(feedbackEdge);
            routingPoints = route.meta.routingPoints ?? [];
            sourceAnchor = route.sourceAnchor;
            targetAnchor = route.targetAnchor;
        }

        const operation: CreateEdgeOperation = {
            kind: "createEdge",
            isOperation: true,
            elementTypeId: this.schema.elementTypeId,
            sourceElementId: this.source.id,
            targetElementId: node.id,
            routingPoints,
            sourceAnchor,
            targetAnchor,
            params: this.schema.params,
            schema: this.schema
        };

        const result: Action[] = [StopCreateEdgeFeedbackAction.create(FEEDBACK_EDGE_ID)];
        if (this.highlightedNodeId != undefined) {
            result.push(SetEdgeEditHighlightAction.create(undefined, undefined));
            this.highlightedNodeId = undefined;
        }
        result.push(operation, EnableDefaultToolsAction.create());

        this.resetState();
        return result;
    }

    /**
     * Cancels current creation state in response to Escape.
     *
     * @returns Actions required to fully reset feedback and return to default tools
     */
    cancelFromEscape(): Action[] {
        const actions: Action[] = [];

        if (this.highlightedNodeId) {
            actions.push(SetEdgeEditHighlightAction.create(undefined, undefined));
            this.highlightedNodeId = undefined;
        }

        if (this.source) {
            actions.push(StopCreateEdgeFeedbackAction.create(FEEDBACK_EDGE_ID));
        }

        this.resetState();
        this.clearMouseDownTracking();
        actions.push(EnableDefaultToolsAction.create());
        return actions;
    }

    /**
     * Finds a connectable GNode from the given target or its parents.
     *
     * @param target The candidate model element
     * @param role The connection role to validate
     * @param edge Optional typed edge instance used for validation
     * @returns The connectable node if validation passes, otherwise undefined
     */
    private findConnectableNode(target: GModelElement, role: "source" | "target", edge?: GEdge): GNode | undefined {
        const element = findParentByFeature(target, isConnectable);
        if (!(element instanceof GNode)) {
            return undefined;
        }

        const referenceEdge = edge ?? this.getFeedbackEdge(target) ?? this.tool.createEdgeFromSchema(this.schema);
        if (!referenceEdge) {
            return role === "source" ? element : undefined;
        }

        if (element.canConnect(referenceEdge, role)) {
            return element;
        }
        return undefined;
    }

    /**
     * Resolves the actual model element under the cursor using the DOM element,
     * which is more precise than the sprotty target during fast mouse movement.
     *
     * @param target The sprotty-provided target
     * @param event The mouse event
     * @returns The best matching model element under the cursor
     */
    private resolveActualTarget(target: GModelElement, event: MouseEvent): GModelElement {
        const elementAtPoint = document.elementFromPoint(event.clientX, event.clientY);
        if (elementAtPoint && target.root) {
            const found = this.tool.elementFinder.findElementByDOMElement(target.root, elementAtPoint);
            if (found) {
                return found;
            }
        }
        return target;
    }

    /**
     * Computes mouse position in the same coordinate system as a node bounds object.
     *
     * @param node The node whose bounds coordinate system should be used
     * @param event The current mouse event
     * @returns Position in node-parent coordinate space
     */
    private getRelativePositionForNode(node: GNode, event: MouseEvent): Point {
        const reference = node instanceof GChildElement ? node.parent : node;
        return getRelativePosition(reference, event);
    }

    /**
     * Gets an appropriate reference element for computing relative position.
     * Uses the source node's parent as coordinate reference for consistency.
     *
     * @param target Fallback position reference element
     * @returns The element used as coordinate reference
     */
    private getPositionReference(target: GModelElement): GModelElement {
        if (this.source instanceof GChildElement) {
            return this.source.parent;
        }
        return target;
    }

    /**
     * Retrieves the feedback edge from the model, if present.
     *
     * @param target Any element in the current root model
     * @returns The active feedback edge or undefined
     */
    private getFeedbackEdge(target: GModelElement): GEdge | undefined {
        const el = target.root.index.getById(FEEDBACK_EDGE_ID);
        return el instanceof GEdge ? el : undefined;
    }

    /**
     * Computes the nearest anchor on a bounding box for a given point.
     * Simplified projection used when no feedback edge is available (phase 1).
     *
     * @param point The point to project
     * @param bounds The target node bounds
     * @returns The projected nearest anchor
     */
    private computeNearestAnchor(
        point: Point,
        bounds: { x: number; y: number; width: number; height: number }
    ): EdgeAnchor {
        const dLeft = Math.abs(point.x - bounds.x);
        const dRight = Math.abs(point.x - (bounds.x + bounds.width));
        const dTop = Math.abs(point.y - bounds.y);
        const dBottom = Math.abs(point.y - (bounds.y + bounds.height));

        const min = Math.min(dLeft, dRight, dTop, dBottom);
        if (min === dTop) {
            return { side: "top", value: Math.max(0, Math.min(1, (point.x - bounds.x) / bounds.width)) };
        } else if (min === dBottom) {
            return { side: "bottom", value: Math.max(0, Math.min(1, (point.x - bounds.x) / bounds.width)) };
        } else if (min === dLeft) {
            return { side: "left", value: Math.max(0, Math.min(1, (point.y - bounds.y) / bounds.height)) };
        } else {
            return { side: "right", value: Math.max(0, Math.min(1, (point.y - bounds.y) / bounds.height)) };
        }
    }

    /**
     * Cancels creation and cleans up feedback.
     *
     * @returns Actions to remove feedback and re-enable default tools
     */
    private cancelCreation(): Action[] {
        const result: Action[] = [StopCreateEdgeFeedbackAction.create(FEEDBACK_EDGE_ID)];
        if (this.highlightedNodeId != undefined) {
            result.push(SetEdgeEditHighlightAction.create(undefined, undefined));
            this.highlightedNodeId = undefined;
        }
        result.push(EnableDefaultToolsAction.create());
        this.resetState();
        this.clearMouseDownTracking();
        return result;
    }

    /**
     * Resets listener state back to phase 1.
     *
     * @returns Nothing
     */
    private resetState(): void {
        this.source = undefined;
        this.schema = undefined;
        this.initialSchema = undefined;
        this.sourceRequest = { requestToken: this.sourceRequest.requestToken + 1 };
        this.targetRequest = { requestToken: this.targetRequest.requestToken + 1 };
    }

    /**
     * Clears temporary mouse-down tracking state.
     *
     * @returns Nothing
     */
    private clearMouseDownTracking(): void {
        this.mouseDownSource = undefined;
        this.mouseDownAt = undefined;
        this.mouseDownGraphPosition = undefined;
        this.mouseDownMoved = false;
        this.sourceWasActiveAtMouseDown = false;
    }

    /**
     * Disposes transient listener state.
     *
     * @returns Nothing
     */
    override dispose(): void {
        const actions: Action[] = [];
        if (this.source) {
            actions.push(StopCreateEdgeFeedbackAction.create(FEEDBACK_EDGE_ID));
        }
        if (this.highlightedNodeId) {
            actions.push(SetEdgeEditHighlightAction.create(undefined, undefined));
        }
        if (actions.length > 0) {
            this.tool.dispatchActions(actions);
        }

        this.highlightedNodeId = undefined;
        this.resetState();
        this.clearMouseDownTracking();
        super.dispose();
    }
}
