import type { SetContextActions } from "@eclipse-glsp/protocol";
import type { IActionDispatcher, GModelRoot, Action, Bounds, Point, IVNodePostprocessor } from "@eclipse-glsp/sprotty";
import type { EditorContextService as EditorContextServiceType } from "@eclipse-glsp/client";
import { EdgeAttachmentPosition, ELEMENT_CONTEXT_ACTIONS_ID } from "@mdeo/protocol-common";
import type { ContextActionItem, AnchorSide } from "@mdeo/protocol-common";
import type { VNode } from "snabbdom";
import { sharedImport } from "../../sharedImport.js";
import { ContextItemsState } from "./contextItemsState.js";
import { IconRegistryKey, type IconRegistry } from "../icon-registry/iconRegistry.js";
import { EdgeRouter, type RouteComputationResult } from "../edge-routing/edgeRouter.js";
import { GNode } from "../../model/node.js";
import { GEdge } from "../../model/edge.js";
import { GNodeViewBase } from "../../views/nodeViewBase.js";
import { ToolStateManager } from "../tool-state/toolStateManager.js";

const { injectable, inject } = sharedImport("inversify");
const {
    html,
    TYPES,
    getAbsoluteBounds,
    SetModelAction,
    UpdateModelAction,
    Point: PointUtil,
    Bounds: BoundsUtil,
    isSelected,
    translatePoint
} = sharedImport("@eclipse-glsp/sprotty");
const { RequestContextActions: RequestContextActionsAction } = sharedImport("@eclipse-glsp/protocol");
const { init, classModule, propsModule, styleModule, eventListenersModule, attributesModule } =
    sharedImport("snabbdom");
const { EditorContextService } = sharedImport("@eclipse-glsp/client");

/**
 * The layout orientation of a context-action rail strip.
 * `"horizontal"` → buttons arranged left-to-right.
 * `"vertical"` → buttons arranged top-to-bottom.
 */
export type ContextActionRailOrientation = "horizontal" | "vertical";

/**
 * Result object returned when rendering a standalone rail into a caller-managed container.
 */
export interface ContextActionRailRenderResult {
    /**
     * The snabbdom virtual-DOM node for the rendered rail.
     */
    vnode: VNode;
    /**
     * The estimated bounding box of the rail in canvas pixels.
     */
    bounds: Bounds;
}

/**
 * Shape of the `args` object included in a server {@link SetContextActions} response.
 */
interface ContextActionsResponseArgs {
    /**
     * Context-actions context identifier echoed back by the server.
     */
    contextId?: string;
    /**
     * JSON-serialised array of {@link ContextActionItem} objects.
     */
    contextItems?: string;
    /**
     * The element ID the server resolved the items for.
     */
    selectedElementId?: string;
}

/**
 * Unified context-actions service that manages the full lifecycle of context-action
 * rail overlays:
 *
 * - Fetches and caches context action items from the GLSP server on demand.
 * - Renders context-action rails as positioned HTML overlays on top of the SVG canvas.
 * - Implements {@link IVNodePostprocessor} so that {@link postUpdate} is called on every
 *   rendered frame (including mid-animation and mid-drag frames), ensuring the overlay
 *   always tracks the selected element precisely.
 *
 * Registered as a singleton. Participates in three framework roles:
 * 1. `IDiagramStartup` — creates the DOM overlay container after model initialisation.
 * 2. `IActionHandler` — clears the item cache on `SetModel` / `UpdateModel` actions.
 * 3. `IVNodePostprocessor` — re-renders the rails every frame via `postUpdate`.
 */
@injectable()
export class ContextActionsUIExtension implements IVNodePostprocessor {
    /**
     * Pixel gap (in model units, scaled by zoom) between the selection rect and
     * the left edge of the node rail.
     */
    static readonly RAIL_GAP = 8;

    /**
     * Distance (in model units, scaled by zoom) between the edge routing line
     * and the near edge of an edge-attached rail.
     */
    static readonly EDGE_DISTANCE_TO_LINE = 2;

    /**
     * GLSP action dispatcher used to send requests and item-click actions.
     */
    @inject(TYPES.IActionDispatcher)
    private actionDispatcher!: IActionDispatcher;

    /**
     * Icon registry for resolving icon VNodes from icon names.
     */
    @inject(IconRegistryKey)
    private iconRegistry!: IconRegistry;

    /**
     * Edge router used to compute edge routes for edge-rail placement.
     */
    @inject(EdgeRouter)
    private edgeRouter!: EdgeRouter;

    /**
     * GLSP editor-context service providing the current model root and event hooks.
     */
    @inject(EditorContextService)
    private editorContextService!: EditorContextServiceType;

    /**
     * Viewer options containing the base div ID for the diagram canvas.
     */
    @inject(TYPES.ViewerOptions)
    private viewerOptions!: { baseDiv: string };

    /**
     * Tool state manager used to suppress the context rail while a creation
     * tool (node or edge) is active, keeping the canvas visually clean.
     */
    @inject(ToolStateManager)
    private toolStateManager!: ToolStateManager;

    /**
     * Cache mapping `"contextId:elementId"` keys to the corresponding item arrays
     * fetched from the server.  Cleared on every `SetModel` / `UpdateModel` action.
     */
    private readonly itemsCache = new Map<string, ContextActionItem[]>();

    /**
     * Set of cache keys for which a server request is currently in-flight,
     * used to prevent duplicate concurrent requests for the same element.
     */
    private readonly pendingRequests = new Set<string>();

    /**
     * Supplementary state object that tracks the last-known selected element and items.
     */
    private readonly contextItemsState = new ContextItemsState();

    /**
     * The overlay `<div>` appended to the base div that hosts all rail VNodes.
     */
    private containerElement: HTMLElement | undefined;

    /**
     * The most recently patched snabbdom VNode for the overlay container.
     */
    private currentVNode: VNode | undefined;

    /**
     * Snabbdom patcher initialised with the modules required by the overlay VNodes.
     */
    private readonly patcher = init([classModule, propsModule, styleModule, eventListenersModule, attributesModule]);

    /**
     * `IDiagramStartup` hook — creates the HTML overlay container after the
     * initial model has been loaded.  Called once by the GLSP framework.
     */
    postModelInitialization(): void {
        this.createContainer();
    }

    /**
     * `IActionHandler` hook — clears the item cache whenever the diagram model
     * is reset (`SetModelAction`) or updated (`UpdateModelAction`) so that stale
     * context items are never shown after a model reload.
     *
     * @param action The action that was dispatched.
     * @param _root  Unused; the model root at the time of dispatch.
     */
    handle(action: Action, _root: GModelRoot | undefined): void {
        if (action.kind === SetModelAction.KIND || action.kind === UpdateModelAction.KIND) {
            const selectedId = this.contextItemsState.getSelectedElementId();
            const preservedItems = selectedId != undefined ? this.itemsCache.get(selectedId) : undefined;

            this.itemsCache.clear();
            this.pendingRequests.clear();

            if (selectedId != undefined && preservedItems != null) {
                this.itemsCache.set(selectedId, preservedItems);
                this.requestContextActions(selectedId);
            } else {
                this.contextItemsState.reset();
            }
        }
    }

    /**
     * `IVNodePostprocessor` hook - noop
     *
     * @param vnode   The virtual DOM node for *element* (returned unchanged).
     * @returns The unchanged *vnode*.
     */
    decorate(vnode: VNode): VNode {
        return vnode;
    }

    /**
     * `IVNodePostprocessor` hook — called once after every render pass, including
     * individual animation frames and drag-feedback frames.
     * Re-renders the context-action rail overlay using the freshly captured {@link currentRenderRoot}
     * so that the overlay always reflects the current visual state of the diagram.
     */
    postUpdate(): void {
        this.doUpdateRails();
    }

    /**
     * Creates and mounts the transparent overlay `<div>` that contains all rail
     * VNodes.  The div sits at `top:0, left:0` inside the base div with
     * `overflow:visible` so that positioned children can extend outside it.
     */
    private createContainer(): void {
        const baseDiv = document.getElementById(this.viewerOptions.baseDiv);
        if (baseDiv == null) {
            return;
        }

        const container = document.createElement("div");
        container.id = `${this.viewerOptions.baseDiv}_context-actions-rail-overlay`;
        container.style.cssText =
            "position:absolute;top:0;left:0;width:0;height:0;overflow:visible;pointer-events:none;z-index:10;";
        baseDiv.appendChild(container);
        this.containerElement = container;

        const placeholder = document.createElement("div");
        container.appendChild(placeholder);
        this.currentVNode = this.patcher(placeholder, html("div", null));
    }

    /**
     * Performs the snabbdom patch to update the overlay VNode tree with rails
     * computed from *root*.  Silently ignores render errors that can occur
     * before the model root is fully initialised.
     */
    private doUpdateRails(): void {
        if (this.containerElement == null || this.currentVNode == null) {
            return;
        }
        const vnode = this.buildRailsVNode(this.editorContextService.modelRoot);
        this.currentVNode = this.patcher(this.currentVNode, vnode);
    }

    /**
     * Returns cached items for the given *contextId* / *elementId* pair.
     * If no entry exists in the cache and an *elementId* is supplied, triggers
     * an asynchronous fetch from the server and returns an empty array in the
     * meantime (the overlay will be re-rendered once the response arrives).
     *
     * @param elementId  The ID of the element to fetch items for.
     * @returns The cached items, or an empty array if the fetch is in-flight.
     */
    private getItems(elementId: string): ContextActionItem[] {
        const cached = this.itemsCache.get(elementId);
        if (cached) {
            return cached;
        }

        if (elementId != undefined) {
            this.requestContextActions(elementId);
            return [];
        }

        return this.contextItemsState.getItems();
    }

    /**
     * Sends a `RequestContextActions` request to the GLSP server for the given
     * element, unless an identical request is already in-flight.
     * Updates the overlay directly when the response arrives.
     *
     * @param elementId The ID of the element to request items for.
     */
    private requestContextActions(elementId: string): void {
        if (this.pendingRequests.has(elementId)) {
            return;
        }

        this.pendingRequests.add(elementId);
        this.contextItemsState.setSelectedElementId(elementId);

        const requestAction = RequestContextActionsAction.create({
            contextId: ELEMENT_CONTEXT_ACTIONS_ID,
            editorContext: { selectedElementIds: [elementId] }
        });

        void this.actionDispatcher
            .request<SetContextActions>(requestAction)
            .then((response) => this.onSetContextActionsResponse(response, elementId))
            .finally(() => this.pendingRequests.delete(elementId));
    }

    /**
     * Processes a `SetContextActions` response from the server: stores the
     * resolved items in the cache and re-renders the overlay immediately.
     *
     * @param response           The server response.
     * @param fallbackElementId  Element ID to use if not present in the response args.
     */
    private onSetContextActionsResponse(response: SetContextActions, fallbackElementId: string): void {
        const args = response.args as ContextActionsResponseArgs | undefined;
        const elementId = typeof args?.selectedElementId === "string" ? args.selectedElementId : fallbackElementId;
        const items = this.resolveItems(response, args?.contextItems);

        this.itemsCache.set(elementId, items);
        this.contextItemsState.setItems(items);
        this.contextItemsState.setSelectedElementId(elementId);
        this.doUpdateRails();
    }

    /**
     * Resolves context-action items from a server response.  Prefers the
     * `response.actions` array (if present) and falls back to deserialising
     * the JSON string in `response.args.contextItems`.
     *
     * @param response            The raw server response.
     * @param serializedArgsItems Optional JSON string from `response.args`.
     * @returns Array of resolved {@link ContextActionItem} objects.
     */
    private resolveItems(response: SetContextActions, serializedArgsItems?: string): ContextActionItem[] {
        const actionItems = (response as unknown as { actions?: unknown[] }).actions;
        if (Array.isArray(actionItems)) {
            return actionItems as ContextActionItem[];
        }
        return this.deserializeItems(serializedArgsItems);
    }

    /**
     * Deserialises a JSON-encoded array of {@link ContextActionItem} objects.
     * Returns an empty array if *serialized* is absent, empty, invalid JSON,
     * or does not decode to an array.
     *
     * @param serialized A JSON string representing a `ContextActionItem[]`.
     * @returns The decoded array, or `[]` on any failure.
     */
    private deserializeItems(serialized?: string): ContextActionItem[] {
        if (typeof serialized !== "string" || serialized.length === 0) {
            return [];
        }
        try {
            const parsed = JSON.parse(serialized);
            return Array.isArray(parsed) ? (parsed as ContextActionItem[]) : [];
        } catch {
            return [];
        }
    }

    /**
     * Returns the node-level (non-positional) context items for *elementId*.
     * Items that have a `position` set belong to edge attachment points and are
     * excluded here.
     *
     * @param elementId The model element ID.
     * @returns Filtered array of non-positional items.
     */
    private nodeItems(elementId: string): ContextActionItem[] {
        return this.getItems(elementId).filter((item) => item.position == undefined);
    }

    /**
     * Builds the root snabbdom VNode for all rail overlays in the diagram.
     * Returns an empty `<div>` if the viewport is unavailable, if anything
     * other than exactly one element is selected, or if a creation tool is
     * currently active (to avoid visual clutter during element placement).
     *
     * @param root The current (potentially mid-animation) model root.
     * @returns A VNode that wraps zero or more positioned overlay divs.
     */
    private buildRailsVNode(root: Readonly<GModelRoot>): VNode {
        if (this.toolStateManager.isCreationMode()) {
            return html("div", null);
        }

        const selection = [...root.index.all().filter((element) => isSelected(element))];

        const railOverlays: VNode[] = [];
        if (selection.length === 1) {
            for (const element of selection) {
                if (element instanceof GNode) {
                    railOverlays.push(...this.buildNodeRailOverlays(element));
                } else if (element instanceof GEdge) {
                    if (!element.reconnectData) {
                        railOverlays.push(...this.buildEdgeRailOverlays(element));
                    }
                }
            }
        }

        return html("div", null, ...railOverlays);
    }

    /**
     * Builds the overlay VNode(s) for a selected node element.
     * The rail is placed to the right of the node, offset by
     * {@link GNodeViewBase.SELECTION_OFFSET} and {@link RAIL_GAP}
     *
     * @param model    The selected node model element.
     * @returns An array containing one rail overlay VNode, or empty when there
     *          are no items for this node.
     */
    private buildNodeRailOverlays(model: Readonly<GNode>): VNode[] {
        const items = this.nodeItems(model.id);
        if (items.length === 0) {
            return [];
        }

        const absoluteBounds = getAbsoluteBounds(model);
        const topRight = BoundsUtil.topRight(absoluteBounds);
        const railPosition: Point = {
            x: topRight.x + ContextActionsUIExtension.RAIL_GAP + GNodeViewBase.SELECTION_OFFSET,
            y: topRight.y - GNodeViewBase.SELECTION_OFFSET
        };

        const overlay = this.buildRailOverlay(items, railPosition, "vertical");
        return overlay != null ? [overlay] : [];
    }

    /**
     * Builds the overlay VNodes for all attachment points of a selected edge.
     * Returns an empty array when the edge route has fewer than two points.
     *
     * @param model    The selected edge model element.
     * @returns Array of rail overlay VNodes, one per non-empty attachment position.
     */
    private buildEdgeRailOverlays(model: Readonly<GEdge>): VNode[] {
        const routeResult = this.edgeRouter.computeRoute(model);
        if (routeResult.route.length < 2) {
            return [];
        }

        const overlays: VNode[] = [];
        const itemsByPosition = new Map<EdgeAttachmentPosition, ContextActionItem[]>([
            [EdgeAttachmentPosition.START, []],
            [EdgeAttachmentPosition.END, []],
            [EdgeAttachmentPosition.MIDDLE, []]
        ]);
        for (const item of this.getItems(model.id)) {
            itemsByPosition.get(item.position ?? EdgeAttachmentPosition.MIDDLE)?.push(item);
        }
        for (const position of [
            EdgeAttachmentPosition.START,
            EdgeAttachmentPosition.END,
            EdgeAttachmentPosition.MIDDLE
        ]) {
            const items = itemsByPosition.get(position)!;
            const overlay = this.buildEdgeRailAtPosition(items, position, model, routeResult);
            if (overlay != null) {
                overlays.push(overlay);
            }
        }
        return overlays;
    }

    /**
     * Builds a single edge-rail overlay for the given *position* (start, end or
     * middle).  Computes the anchor point, determines orientation based on the
     * edge direction at that point, positions the rail away from the routing
     * line by {@link EDGE_DISTANCE_TO_LINE} (scaled by zoom), and delegates to
     * {@link buildRailOverlay}.
     *
     * @param items        Items to display in the rail.
     * @param position     The edge-attachment position.
     * @param routeResult  The computed route for the edge.
     * @returns The rail overlay VNode, or `undefined` when *items* is empty or
     *          the anchor cannot be determined.
     */
    private buildEdgeRailAtPosition(
        items: ContextActionItem[],
        position: EdgeAttachmentPosition,
        model: Readonly<GEdge>,
        routeResult: RouteComputationResult
    ): VNode | undefined {
        if (items.length === 0) {
            return undefined;
        }

        const anchor = this.edgeAnchorInfo(position, routeResult);
        if (anchor == null) {
            return undefined;
        }

        const isHorizontal = Math.abs(anchor.dx) > Math.abs(anchor.dy);
        const orientation: ContextActionRailOrientation =
            position === EdgeAttachmentPosition.MIDDLE
                ? isHorizontal
                    ? "horizontal"
                    : "vertical"
                : isHorizontal
                  ? "vertical"
                  : "horizontal";

        const root = this.editorContextService.modelRoot;
        const absoluteAnchor = root.localToParent(translatePoint(anchor.anchorPoint, model, root));

        const edgeDirSign = isHorizontal ? (anchor.dx >= 0 ? 1 : -1) : anchor.dy >= 0 ? 1 : -1;
        const oppositeSign = -edgeDirSign;

        const extraOffset = position !== EdgeAttachmentPosition.MIDDLE ? 12 : 0;
        const cssTransform = this.buildEdgeRailCssTransform(isHorizontal, oppositeSign, extraOffset);

        return this.buildRailOverlay(items, absoluteAnchor, orientation, cssTransform);
    }

    /**
     * Builds the CSS `transform: translate(...)` string that centres and offsets an edge-rail
     * relative to its anchor point, using percentage values so that no prior knowledge of the
     * element's rendered size is required.
     *
     * @param isHorizontal Whether the edge segment at the anchor is horizontal.
     * @param oppositeSign −1 when the rail should be placed against the edge direction, +1 otherwise.
     * @returns A CSS transform string.
     */
    private buildEdgeRailCssTransform(isHorizontal: boolean, oppositeSign: number, extraOffset: number = 0): string {
        const offset = ContextActionsUIExtension.EDGE_DISTANCE_TO_LINE + extraOffset;
        if (isHorizontal) {
            if (oppositeSign > 0) {
                return `translate(${offset}px, -50%)`;
            } else {
                return `translate(calc(-100% - ${offset}px), -50%)`;
            }
        } else {
            if (oppositeSign > 0) {
                return `translate(-50%, ${offset}px)`;
            } else {
                return `translate(-50%, calc(-100% - ${offset}px))`;
            }
        }
    }

    /**
     * Wraps a rail VNode in an absolutely-positioned container div at the given
     * canvas coordinates.  Returns `undefined` when the inner rail VNode is
     * `undefined` (i.e., there are no items to render).
     *
     * @param items          Items to pass to {@link buildRailVNode}.
     * @param canvasPosition Top-left position in canvas (CSS pixel) coordinates.
     * @param orientation    Layout orientation of the rail.
     * @param transform      Optional CSS transform string to apply to the container for fine-tuning position.
     * @returns A positioned overlay VNode, or `undefined`.
     */
    private buildRailOverlay(
        items: ContextActionItem[],
        canvasPosition: Point,
        orientation: ContextActionRailOrientation,
        transform?: string
    ): VNode | undefined {
        const railContent = this.buildRailVNode(items, orientation);
        if (railContent == null) {
            return undefined;
        }
        const style: Record<string, string> = {
            position: "absolute",
            left: `${canvasPosition.x}px`,
            top: `${canvasPosition.y}px`,
            "pointer-events": "none"
        };
        if (transform != null) {
            style["transform"] = transform;
        }
        return html("div", { style }, railContent);
    }

    /**
     * Computes the anchor point and edge direction vector for an edge-rail at
     * the given *position* along the route.
     *
     * @param position    The attachment position (start, end, or middle).
     * @param routeResult The computed route for the edge.
     * @returns An object with the anchor point, direction deltas, and optional
     *          anchor side, or `undefined` when the route has fewer than 2 points.
     */
    private edgeAnchorInfo(
        position: EdgeAttachmentPosition,
        routeResult: RouteComputationResult
    ): { anchorPoint: Point; dx: number; dy: number; anchorSide?: AnchorSide } | undefined {
        const route = routeResult.route;
        if (route.length < 2) {
            return undefined;
        }

        if (position === EdgeAttachmentPosition.START) {
            const anchorPoint = route[0];
            const anchorSide = routeResult.sourceAnchor?.side;
            if (anchorSide != null) {
                const dir = this.directionFromSide(anchorSide);
                return { anchorPoint, dx: dir.x, dy: dir.y, anchorSide };
            }
            return { anchorPoint, dx: route[1].x - route[0].x, dy: route[1].y - route[0].y, anchorSide };
        }

        if (position === EdgeAttachmentPosition.END) {
            const anchorPoint = route[route.length - 1];
            const anchorSide = routeResult.targetAnchor?.side;
            if (anchorSide != null) {
                const dir = this.directionFromSide(anchorSide);
                return { anchorPoint, dx: dir.x, dy: dir.y, anchorSide };
            }
            return {
                anchorPoint,
                dx: route[route.length - 2].x - route[route.length - 1].x,
                dy: route[route.length - 2].y - route[route.length - 1].y,
                anchorSide
            };
        }

        if (position === EdgeAttachmentPosition.MIDDLE) {
            const middleIndex = Math.floor(route.length / 2);
            const anchorPoint = PointUtil.linear(route[middleIndex - 1], route[middleIndex], 0.5);
            return {
                anchorPoint,
                dx: route[middleIndex].x - anchorPoint.x,
                dy: route[middleIndex].y - anchorPoint.y
            };
        }

        return undefined;
    }

    /**
     * Returns a unit direction vector pointing away from a node on the given
     * anchor *side*.
     *
     * @param side The side of the node the edge is anchored to.
     * @returns A `{x, y}` unit vector.
     */
    private directionFromSide(side: AnchorSide): Point {
        switch (side) {
            case "top":
                return { x: 0, y: -1 };
            case "bottom":
                return { x: 0, y: 1 };
            case "left":
                return { x: -1, y: 0 };
            case "right":
                return { x: 1, y: 0 };
        }
    }

    /**
     * Builds the inner snabbdom VNode for a context-action rail strip.
     * Returns `undefined` when *items* is empty so the caller can skip rendering
     * the outer wrapper.
     *
     * @param items       The items to render as buttons.
     * @param orientation The layout orientation.
     * @returns A styled flex-container VNode, or `undefined`.
     */
    private buildRailVNode(items: ContextActionItem[], orientation: ContextActionRailOrientation): VNode | undefined {
        if (items.length === 0) {
            return undefined;
        }
        return html(
            "div",
            {
                class: {
                    flex: true,
                    "flex-row": orientation === "horizontal",
                    "flex-col": orientation === "vertical",
                    "items-center": true,
                    "gap-1": true,
                    "rounded-md": true,
                    border: true,
                    "border-border": true,
                    "bg-toolbox": true,
                    "shadow-md": true,
                    "pointer-events-auto": true,
                    "px-1": true,
                    "py-1": true,
                    "w-fit": orientation === "horizontal",
                    "h-fit": orientation === "vertical"
                }
            },
            ...this.renderItems(items, orientation)
        );
    }

    /**
     * Maps, sorts, and filters *items* into an array of rendered VNodes.
     *
     * @param items       The items to render.
     * @param orientation The orientation passed to each item renderer.
     * @returns Array of non-null VNodes.
     */
    private renderItems(items: ContextActionItem[], orientation: ContextActionRailOrientation): VNode[] {
        return this.sortItems(items)
            .map((item) => this.renderItem(item, orientation))
            .filter((v): v is VNode => v != null);
    }

    /**
     * Sorts *items* by their `sortString` field using numeric-aware locale
     * comparison.  Items without a `sortString` are appended in their original
     * order after those that have one.
     *
     * @param items The items to sort.
     * @returns A new sorted array.
     */
    private sortItems(items: ContextActionItem[]): ContextActionItem[] {
        const withSort = items.filter((i) => i.sortString != null);
        const withoutSort = items.filter((i) => i.sortString == null);
        withSort.sort((a, b) => a.sortString!.localeCompare(b.sortString!, undefined, { numeric: true }));
        return [...withSort, ...withoutSort];
    }

    /**
     * Renders a single context-action item as either a menu-button (when the
     * item has children) or a plain icon-button.
     *
     * @param item        The item to render.
     * @param orientation The orientation of the parent rail (forwarded to
     *                    the submenu placement logic).
     * @returns A VNode for the item, or `undefined`.
     */
    private renderItem(item: ContextActionItem, orientation: ContextActionRailOrientation): VNode | undefined {
        return item.children && item.children.length > 0
            ? this.renderMenuButton(item, orientation)
            : this.renderIconButton(item);
    }

    /**
     * Renders a leaf context-action item as a small square icon button.
     * Dispatches the item's action through the GLSP action dispatcher on click.
     *
     * @param item The leaf item to render.
     * @returns A `<button>` VNode.
     */
    private renderIconButton(item: ContextActionItem): VNode {
        const iconVNode = this.renderIcon(item);
        return html(
            "button",
            {
                class: {
                    "inline-flex": true,
                    "h-8": true,
                    "w-8": true,
                    "items-center": true,
                    "justify-center": true,
                    "rounded-md": true,
                    "transition-colors": true,
                    "duration-150": true,
                    "hover:bg-accent": true,
                    "hover:text-accent-foreground": true,
                    "focus-visible:outline-none": true,
                    "focus-visible:ring-2": true,
                    "focus-visible:ring-ring": true,
                    "disabled:pointer-events-none": true,
                    "disabled:opacity-50": true,
                    "cursor-pointer": true
                },
                attrs: { "data-item-id": item.id, type: "button", title: item.label || "" },
                on: {
                    click: (event: MouseEvent) => {
                        event.preventDefault();
                        event.stopPropagation();
                        this.handleItemClick(item);
                    }
                }
            },
            ...(iconVNode ? [iconVNode] : [])
        );
    }

    /**
     * Renders a context-action item that has children as a button with a
     * CSS-hover-driven dropdown submenu.  A transparent bridge element fills
     * the gap between the button and the submenu to keep the hover state alive
     * while the pointer travels between them.
     *
     * @param item        The parent item with children.
     * @param orientation The orientation of the parent rail.
     * @returns A grouped `<div>` VNode containing the button, bridge, and menu.
     */
    private renderMenuButton(item: ContextActionItem, orientation: ContextActionRailOrientation): VNode {
        const iconVNode = this.renderIcon(item);
        const menuVNode = this.renderContextMenu(item.children ?? [], orientation);

        const bridgeVNode = html("div", {
            class: {
                absolute: true,
                "left-full": orientation === "vertical",
                "top-0": orientation === "vertical",
                "h-full": orientation === "vertical",
                "w-3": orientation === "vertical",
                "-ml-1": orientation === "vertical",
                "top-full": orientation === "horizontal",
                "left-0": orientation === "horizontal",
                "w-full": orientation === "horizontal",
                "h-3": orientation === "horizontal",
                "-mt-1": orientation === "horizontal"
            }
        });

        return html(
            "div",
            {
                class: { group: true, relative: true, flex: true, "items-center": true },
                attrs: { "data-item-id": item.id }
            },
            html(
                "button",
                {
                    class: {
                        "inline-flex": true,
                        "h-8": true,
                        "w-8": true,
                        "items-center": true,
                        "justify-center": true,
                        "rounded-md": true,
                        "transition-colors": true,
                        "duration-150": true,
                        "hover:bg-accent": true,
                        "hover:text-accent-foreground": true,
                        "focus-visible:outline-none": true,
                        "focus-visible:ring-2": true,
                        "focus-visible:ring-ring": true,
                        "cursor-pointer": true
                    },
                    attrs: { type: "button", title: item.label || "" },
                    on: item.action
                        ? {
                              click: (event: MouseEvent) => {
                                  event.preventDefault();
                                  event.stopPropagation();
                                  this.handleItemClick(item);
                              }
                          }
                        : {}
                },
                ...(iconVNode ? [iconVNode] : [])
            ),
            bridgeVNode,
            menuVNode
        );
    }

    /**
     * Builds a CSS-hover-driven dropdown menu VNode for the given sub-items.
     * The menu is hidden by default and revealed on `group-hover` of the
     * parent button wrapper.
     *
     * @param items       The child items to render as menu rows.
     * @param orientation The orientation of the parent rail, used to determine
     *                    whether the menu opens to the right or below.
     * @returns A positioned dropdown `<div>` VNode.
     */
    private renderContextMenu(items: ContextActionItem[], orientation: ContextActionRailOrientation): VNode {
        const positionClasses: Record<string, boolean> =
            orientation === "vertical"
                ? { "left-full": true, "top-0": true, "ml-1": true }
                : { "top-full": true, "left-0": true, "mt-1": true };

        const menuItems = items.map((item) => {
            const iconVNode = this.renderIcon(item);
            const labelNode = item.label ? html("span", { class: { truncate: true } }, item.label) : undefined;
            const children: VNode[] = [];
            if (iconVNode) children.push(iconVNode);
            if (labelNode) children.push(labelNode);

            return html(
                "button",
                {
                    class: {
                        flex: true,
                        "w-full": true,
                        "items-center": true,
                        "gap-2": true,
                        "rounded-sm": true,
                        "px-2": true,
                        "py-1.5": true,
                        "text-sm": true,
                        "cursor-pointer": true,
                        "transition-colors": true,
                        "hover:bg-accent": true,
                        "hover:text-accent-foreground": true,
                        "focus-visible:outline-none": true,
                        "focus-visible:ring-2": true,
                        "focus-visible:ring-ring": true
                    },
                    attrs: { "data-item-id": item.id, type: "button", title: item.label || "" },
                    on: {
                        click: (event: MouseEvent) => {
                            event.preventDefault();
                            event.stopPropagation();
                            this.handleItemClick(item);
                        }
                    }
                },
                ...children
            );
        });

        return html(
            "div",
            {
                class: {
                    invisible: true,
                    absolute: true,
                    ...positionClasses,
                    "z-50": true,
                    flex: true,
                    "min-w-44": true,
                    "max-w-72": true,
                    "flex-col": true,
                    "gap-0.5": true,
                    "rounded-md": true,
                    border: true,
                    "border-border": true,
                    "bg-popover": true,
                    "p-1": true,
                    "text-popover-foreground": true,
                    "opacity-0": true,
                    "shadow-md": true,
                    "pointer-events-none": true,
                    "transition-opacity": true,
                    "duration-150": true,
                    "group-hover:visible": true,
                    "group-hover:opacity-100": true,
                    "group-hover:pointer-events-auto": true
                }
            },
            ...menuItems
        );
    }

    /**
     * Resolves and returns the SVG icon VNode for *item* via the icon registry.
     * Returns `undefined` when the item has no icon or the registry returns nothing.
     *
     * @param item The context-action item.
     * @returns A VNode for the icon, or `undefined`.
     */
    private renderIcon(item: ContextActionItem): VNode | undefined {
        if (!item.icon) {
            return undefined;
        }
        const iconName = typeof item.icon === "string" ? item.icon : item.icon.name;
        const cssClasses = typeof item.icon === "string" ? "" : (item.icon.cssClasses ?? "");
        return this.iconRegistry.getIcon(iconName, 16, cssClasses) as VNode | undefined;
    }

    /**
     * Dispatches the action associated with *item* through the GLSP action
     * dispatcher.  Does nothing if *item* has no action attached.
     *
     * @param item The context-action item that was clicked.
     */
    private handleItemClick(item: ContextActionItem): void {
        if (item.action) {
            this.actionDispatcher.dispatch(item.action);
        }
    }
}
