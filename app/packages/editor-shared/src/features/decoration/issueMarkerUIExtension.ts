import type {
    DOMHelper,
    GModelRoot,
    IActionHandler,
    ICommand,
    IVNodePostprocessor,
    Point
} from "@eclipse-glsp/sprotty";
import type { Action } from "@eclipse-glsp/sprotty";
import type { EditorContextService as EditorContextServiceType } from "@eclipse-glsp/client";
import type { GIssueMarker } from "../../model/issueMarker.js";
import type { VNode } from "snabbdom";
import { sharedImport } from "../../sharedImport.js";
import { ScrollViewState, generateScrollView } from "../toolbox/views/scrollView.js";
import { isIssueMarker, getSeverityDisplayConfig, ISSUE_MARKER_SIZE } from "./issueMarker.js";
import { IssueMarkerHoverEnterAction, IssueMarkerHoverLeaveAction } from "./issueMarkerActions.js";

const { injectable, inject } = sharedImport("inversify");
const { html, TYPES } = sharedImport("@eclipse-glsp/sprotty");
const { init, classModule, propsModule, styleModule, eventListenersModule, attributesModule } =
    sharedImport("snabbdom");
const { EditorContextService } = sharedImport("@eclipse-glsp/client");

/**
 * DOM overlay UI extension that renders hover popup panels for GLSP issue markers.
 *
 * ## Architecture
 *
 * Badge SVG icons are rendered by the parent node/edge view ({@link GNodeViewBase} /
 * {@link GEdgeView}).  The badge SVG contains a transparent `<rect>` (added by
 * {@link CustomGIssueMarkerView}) that enables pointer events so GLSP mouse listeners
 * can detect hover: sprotty maps pointer events on the rect to the `GIssueMarker`
 * model element, which {@link IssueMarkerMouseListener} uses directly as the hover target.
 *
 * When the pointer enters a badge, {@link IssueMarkerMouseListener} dispatches
 * {@link IssueMarkerHoverEnterAction} with the marker's model ID.  This extension
 * handles that action by looking up the marker's live DOM element via
 * {@link DOMHelper} and reading its {@link Element.getBoundingClientRect} to position
 * the popup — no separate badge-position map is maintained.
 *
 * Only **one** popup panel is rendered at a time — for the currently active marker.
 * The panel has `pointer-events: auto`, so while the pointer is over the popup the SVG
 * receives no `mousemove` events and no leave action is dispatched.  The popup is hidden
 * only when the pointer moves off both the badge and the popup.
 *
 * ## Framework roles
 *
 * Registered as a singleton via {@link decorationModule}. Participates in three
 * framework roles:
 * 1. `IDiagramStartup` — creates the overlay container after model initialisation.
 * 2. `IVNodePostprocessor` — re-renders the overlay every frame so popup positioning
 *    tracks diagram changes (pan, zoom, element movement).
 * 3. `IActionHandler` — handles {@link IssueMarkerHoverEnterAction} and
 *    {@link IssueMarkerHoverLeaveAction} dispatched by {@link IssueMarkerMouseListener}.
 */
@injectable()
export class IssueMarkerUIExtension implements IVNodePostprocessor, IActionHandler {
    /**
     * Viewer options containing the base div ID for the diagram canvas.
     */
    @inject(TYPES.ViewerOptions)
    private viewerOptions!: { baseDiv: string };

    /**
     * Sprotty DOM helper used to resolve the DOM element ID for a model element.
     */
    @inject(TYPES.DOMHelper)
    private domHelper!: DOMHelper;

    /**
     * GLSP editor-context service providing the current model root.
     */
    @inject(EditorContextService)
    private editorContextService!: EditorContextServiceType;

    /**
     * The overlay `<div>` appended to the base div that hosts all popup VNodes.
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
     * Per-marker scroll state, keyed by `marker.id`.
     *
     * Using a `Map` (rather than creating a new {@link ScrollViewState} on every render)
     * ensures the scroll position and thumb visibility are preserved across frames.
     * Entries are pruned when the corresponding marker is no longer present in the
     * model (see {@link buildOverlayVNode}).
     */
    private readonly scrollStates = new Map<string, ScrollViewState>();

    /**
     * ID of the marker whose popup is currently shown in the overlay, or
     * `undefined` when no popup has been opened yet.
     *
     * Set when {@link IssueMarkerHoverEnterAction} is handled; cleared by
     * {@link IssueMarkerHoverLeaveAction} or when the marker is removed from the model.
     * Moving to a different badge replaces this value rather than clearing it.
     */
    private activeMarkerId: string | undefined;

    /**
     * Fixed popup width in CSS pixels (matches Tailwind `w-48` = 12 rem × 16 px).
     */
    private static readonly POPUP_WIDTH = 192;

    /**
     * Maximum popup height in CSS pixels (matches Tailwind `max-h-32` = 8 rem × 16 px).
     */
    private static readonly POPUP_MAX_HEIGHT = 128;

    /**
     * `IDiagramStartup` hook — creates the HTML overlay container once, after the
     * initial model has been loaded.
     */
    postModelInitialization(): void {
        this.createContainer();
    }

    /**
     * `IVNodePostprocessor` hook — no-op decoration pass.
     *
     * @param vnode The virtual DOM node (returned unchanged).
     * @returns The same `vnode` unchanged.
     */
    decorate(vnode: VNode): VNode {
        return vnode;
    }

    /**
     * `IVNodePostprocessor` hook — called once after every render pass.
     * Re-renders the popup overlay so badge positions track diagram changes.
     */
    postUpdate(): void {
        this.doUpdateOverlay();
    }

    /**
     * `IActionHandler` hook — handles hover enter/leave actions from
     * {@link IssueMarkerMouseListener}.
     *
     * - {@link IssueMarkerHoverEnterAction}: activates the identified marker's
     *   popup and applies the always-visible class.
     * - {@link IssueMarkerHoverLeaveAction}: removes the always-visible class so
     *   the popup persists only while CSS `:hover` is active (pointer over popup).
     *
     * @param action The action to handle.
     */
    handle(action: Action): void | Action | ICommand {
        if (IssueMarkerHoverEnterAction.is(action)) {
            this.activeMarkerId = action.markerId;
            this.doUpdateOverlay();
        } else if (IssueMarkerHoverLeaveAction.is(action)) {
            if (this.activeMarkerId === action.markerId) {
                this.activeMarkerId = undefined;
                this.doUpdateOverlay();
            }
        }
    }

    /**
     * Creates and mounts the transparent overlay `<div>` inside the base div.
     * The div sits at `top:0, left:0` with `overflow:visible` so that popup
     * children can extend outside the container's zero-width/height box.
     */
    private createContainer(): void {
        const baseDiv = document.getElementById(this.viewerOptions.baseDiv);
        if (baseDiv == null) {
            return;
        }

        const container = document.createElement("div");
        container.id = `${this.viewerOptions.baseDiv}_issue-marker-overlay`;
        container.style.cssText =
            "position:absolute;top:0;left:0;width:0;height:0;overflow:visible;pointer-events:none;z-index:11;";
        baseDiv.appendChild(container);
        this.containerElement = container;

        const placeholder = document.createElement("div");
        container.appendChild(placeholder);
        this.currentVNode = this.patcher(placeholder, html("div", null));
    }

    /**
     * Patches the overlay VNode tree with fresh popup positions derived from the
     * current model root.  Silently ignores frames where the container is not yet
     * available (before model initialisation).
     */
    private doUpdateOverlay(): void {
        if (this.containerElement == null || this.currentVNode == null) {
            return;
        }
        const vnode = this.buildOverlayVNode(this.editorContextService.modelRoot);
        this.currentVNode = this.patcher(this.currentVNode, vnode);
    }

    /**
     * Looks up the active marker in the model root and renders one popup panel
     * positioned using the marker's live DOM bounding rect.  Prunes stale entries
     * from {@link scrollStates} for markers no longer active.
     *
     * @param root The current model root.
     * @returns A wrapper VNode that contains at most one popup panel child.
     */
    private buildOverlayVNode(root: Readonly<GModelRoot>): VNode {
        let popupPanelVNode: VNode | undefined;

        if (this.activeMarkerId != null) {
            const marker = root.index.getById(this.activeMarkerId);
            if (marker != null && isIssueMarker(marker)) {
                const badgePos = this.getBadgePositionFromDOM(marker);
                if (badgePos != null) {
                    popupPanelVNode = this.buildPopupPanel(marker, badgePos.x, badgePos.y);
                }
            } else {
                this.activeMarkerId = undefined;
            }
        }

        for (const key of this.scrollStates.keys()) {
            if (key !== this.activeMarkerId) {
                this.scrollStates.delete(key);
            }
        }

        return html("div", null, popupPanelVNode);
    }

    /**
     * Resolves the badge's top-left position in canvas CSS pixels by reading the
     * marker's DOM bounding rect and subtracting the base div's rect.
     *
     * Uses {@link DOMHelper.createUniqueDOMElementId} to obtain the element's DOM ID
     * without duplicating the ID-construction logic.
     *
     * @param marker The issue marker model element.
     * @returns The badge top-left in canvas CSS pixels, or `undefined` when the
     *   DOM element is not found.
     */
    private getBadgePositionFromDOM(marker: GIssueMarker): Point | undefined {
        const domId = this.domHelper.createUniqueDOMElementId(marker);

        const baseDiv = this.containerElement?.parentElement;
        if (baseDiv == undefined) {
            return undefined;
        }

        const rectChild = baseDiv.querySelector(`#${domId} rect`);
        if (rectChild == undefined) {
            return undefined;
        }

        const markerRect = rectChild.getBoundingClientRect();
        const baseDivRect = baseDiv.getBoundingClientRect();
        return {
            x: markerRect.left - baseDivRect.left,
            y: markerRect.top - baseDivRect.top
        };
    }

    /**
     * Builds the popup panel for the active issue marker, absolutely positioned
     * within the overlay container.
     *
     * The panel has `pointer-events: auto` so that while the pointer is over it,
     * SVG mouse events are blocked and no {@link IssueMarkerHoverLeaveAction} is
     * dispatched — the popup stays visible until the pointer leaves both the badge
     * and the panel.
     *
     * The panel is positioned so it stays within the diagram canvas boundaries:
     * it prefers to extend **leftward** and **downward** from the badge, but flips
     * to extend rightward or upward when space is insufficient.
     *
     * @param marker  The active issue marker whose issues are listed.
     * @param badgeX  Left edge of the badge in canvas CSS pixels.
     * @param badgeY  Top edge of the badge in canvas CSS pixels.
     * @returns A VNode for the popup panel.
     */
    private buildPopupPanel(marker: GIssueMarker, badgeX: number, badgeY: number): VNode {
        if (!this.scrollStates.has(marker.id)) {
            this.scrollStates.set(marker.id, new ScrollViewState());
        }
        const scrollState = this.scrollStates.get(marker.id)!;
        const issueRows = marker.issues.map((issue) => this.buildIssueRow(issue));

        const containerEl = this.containerElement?.parentElement;
        const containerWidth = containerEl?.clientWidth ?? Infinity;
        const containerHeight = containerEl?.clientHeight ?? Infinity;

        const topPx =
            badgeY + ISSUE_MARKER_SIZE + IssueMarkerUIExtension.POPUP_MAX_HEIGHT <= containerHeight
                ? badgeY + ISSUE_MARKER_SIZE
                : Math.max(0, badgeY - IssueMarkerUIExtension.POPUP_MAX_HEIGHT);

        const spaceOnLeft = badgeX + ISSUE_MARKER_SIZE;
        let leftPx: number;
        if (spaceOnLeft >= IssueMarkerUIExtension.POPUP_WIDTH) {
            leftPx = badgeX + ISSUE_MARKER_SIZE - IssueMarkerUIExtension.POPUP_WIDTH;
        } else if (badgeX + IssueMarkerUIExtension.POPUP_WIDTH <= containerWidth) {
            leftPx = badgeX;
        } else {
            leftPx = Math.max(0, containerWidth - IssueMarkerUIExtension.POPUP_WIDTH);
        }

        return html(
            "div",
            {
                key: marker.id,
                class: {
                    "bg-popover": true,
                    border: true,
                    "border-border": true,
                    "rounded-md": true,
                    "shadow-md": true,
                    "text-xs": true,
                    "pointer-events-auto": true
                },
                style: {
                    position: "absolute",
                    left: `${leftPx}px`,
                    top: `${topPx}px`,
                    width: `${IssueMarkerUIExtension.POPUP_WIDTH}px`,
                    "max-height": `${IssueMarkerUIExtension.POPUP_MAX_HEIGHT}px`,
                    overflow: "hidden"
                }
            },
            generateScrollView(scrollState, () => issueRows)
        );
    }

    /**
     * Returns whether the given element is contained within the popup overlay container.
     *
     * Used by {@link IssueMarkerMouseListener} to suppress `mouseOut` leave actions
     * when the pointer moves from the SVG directly into the popup panel.
     *
     * @param el The element to test.
     * @returns `true` if `el` is a descendant of the overlay container.
     */
    isPopupElement(el: Element): boolean {
        return this.containerElement?.contains(el) ?? false;
    }

    /**
     * Renders a single issue row inside the popup panel.
     *
     * Color and label are derived from {@link getSeverityDisplayConfig} so the
     * display is consistent with the toolbox error view.
     *
     * @param issue The issue object to display (provides `severity` and `message`).
     * @returns A `<div>` VNode for the issue row.
     */
    private buildIssueRow(issue: { severity: string; message: string }): VNode {
        const { textClass, label } = getSeverityDisplayConfig(issue.severity);
        return html(
            "div",
            {
                class: {
                    "py-1.5": true,
                    "px-2": true,
                    "select-text": true,
                    "break-words": true,
                    "border-b": true,
                    "border-border": true,
                    "last:border-b-0": true,
                    [textClass]: true
                }
            },
            `${label}: ${issue.message}`
        );
    }
}
