import type { GModelElement, Point, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import { EdgeEditHighlightState, type GNode } from "../model/node.js";
import { findViewportZoom } from "../base/findViewportZoom.js";
import type { VNode } from "snabbdom";
import { renderCreateEdgeEndpoint } from "./edgeView.js";
import { isIssueMarker, ISSUE_MARKER_SIZE } from "../features/decoration/issueMarker.js";
import type { GIssueMarker } from "../model/issueMarker.js";

const { injectable, inject } = sharedImport("inversify");
const { svg, ShapeView } = sharedImport("@eclipse-glsp/sprotty");
const { ResizeHandleLocation } = sharedImport("@eclipse-glsp/client");

/**
 * Configuration for a resize handle
 */
interface ResizeHandleConfig {
    location: string;
    startPos: number;
    endPos: number;
}

/**
 * Base class for node views that provides selection rectangle, resize handles, and reconnect
 * target border, without imposing any particular render strategy.
 *
 * Extend this class when you want to fully override render() yourself (e.g. pure SVG nodes).
 * For nodes rendered via a HTML foreignObject use {@link GNodeView} instead.
 */
@injectable()
export abstract class GNodeViewBase extends ShapeView {
    /**
     * The offset of the selection rectangle
     */
    static readonly SELECTION_OFFSET = 6;

    /**
     * The size of the inner point
     */
    static readonly INNER_POINT_SIZE = 8;

    @inject(EdgeEditHighlightState) protected highlightState!: EdgeEditHighlightState;

    /**
     * Configuration for all resize handles (edges and corners)
     */
    private static readonly RESIZE_HANDLES: ResizeHandleConfig[] = [
        { location: ResizeHandleLocation.Right, startPos: 1, endPos: 2 },
        { location: ResizeHandleLocation.Left, startPos: 3, endPos: 0 },
        { location: ResizeHandleLocation.Top, startPos: 0, endPos: 1 },
        { location: ResizeHandleLocation.Bottom, startPos: 2, endPos: 3 },
        { location: ResizeHandleLocation.TopLeft, startPos: 0, endPos: 0 },
        { location: ResizeHandleLocation.TopRight, startPos: 1, endPos: 1 },
        { location: ResizeHandleLocation.BottomRight, startPos: 2, endPos: 2 },
        { location: ResizeHandleLocation.BottomLeft, startPos: 3, endPos: 3 }
    ];

    /**
     * Renders all control elements (selection rectangle and resize handles) for the node that should be rendered behind the node.
     *
     * @param model The model of the element
     * @returns The control elements
     */
    /**
     * Splits the node's children into regular children and issue marker children.
     * Issue markers are excluded from the regular children so they can be rendered
     * separately as SVG overlay badges via {@link renderIssueMarkers}.
     *
     * @param model The node model whose children are partitioned.
     * @returns An object with `children` (non-marker elements) and `markers` (issue markers).
     */
    protected splitChildren(model: Readonly<GNode>): { children: GModelElement[]; markers: GIssueMarker[] } {
        const children: GModelElement[] = [];
        const markers: GIssueMarker[] = [];
        for (const child of model.children) {
            if (isIssueMarker(child)) {
                markers.push(child);
            } else {
                children.push(child);
            }
        }
        return { children, markers };
    }

    protected renderBackgroundControlElements(model: Readonly<GNode>): VNode[] {
        const result: VNode[] = [];

        if (this.highlightState.nodeId === model.id) {
            result.push(...this.renderEdgeEditHighlightBorder(model));
        }

        if (!model.selected) {
            return result;
        }

        const zoom = findViewportZoom(model);
        return [...result, ...this.renderSelectedRect(model, zoom), ...this.renderResizeHandles(model, zoom)];
    }

    /**
     * Renders all control elements for the node that should be rendered in front of the node.
     * Context action rails are no longer rendered here; they are handled by
     * {@link ContextActionsUIExtension} as a DOM overlay.
     *
     * @param model The model of the element
     * @returns The foreground control VNodes
     */
    protected renderForegroundControlElements(model: Readonly<GNode>): VNode[] {
        const result: VNode[] = [];

        const highlight = this.highlightState.nodeId === model.id ? this.highlightState.highlight : undefined;
        if (highlight?.anchorPosition != undefined) {
            result.push(this.renderEdgeEditHighlightAnchor(model, highlight.anchorPosition));
        }

        return result;
    }

    /**
     * Renders issue marker badges for all GIssueMarker children of the node.
     *
     * Each badge is placed at the top-right corner of the node, centered on that corner
     * so it overlaps both the node interior and the surrounding canvas.  The badge size
     * is kept constant in physical CSS pixels regardless of the current zoom level.
     *
     * @param markers The pre-split issue marker children (from {@link splitChildren}).
     * @param model The node model, used for bounds and zoom positioning.
     * @param context The rendering context used to render the marker view.
     * @returns An array of SVG VNodes for the issue marker badges (empty when there are none).
     */
    protected renderIssueMarkers(
        markers: readonly GIssueMarker[],
        model: Readonly<GNode>,
        context: RenderingContext
    ): VNode[] {
        if (markers.length === 0) {
            return [];
        }

        const zoom = findViewportZoom(model);
        const iconSvgSize = ISSUE_MARKER_SIZE / zoom;

        // Pick the first marker (GLSP guarantees at most one GIssueMarker per element
        // with the highest-priority severity already resolved inside it).
        const markerVNode = context.renderElement(markers[0]);
        if (markerVNode == undefined) {
            return [];
        }

        // Position the badge centred on the top-right corner of the node.
        const x = model.bounds.width - iconSvgSize / 2;
        const y = -iconSvgSize / 2;

        return [svg("g", { attrs: { transform: `translate(${x}, ${y})` } }, markerVNode)];
    }

    /**
     * Generates a rectangle that is displayed if the element is selected.
     * Does NOT check if the element is selected.
     *
     * @param model The model of the element
     * @param zoom The zoom level of the diagram
     * @returns The rectangle and its children
     */
    private renderSelectedRect(model: Readonly<GNode>, zoom: number): VNode[] {
        const result: VNode[] = [
            svg("rect", {
                class: {
                    "stroke-primary": true,
                    "stroke-[1.5px]": true,
                    "fill-none": true,
                    "non-scaling-stroke": true,
                    "pointer-events-none": true
                },
                attrs: {
                    ...this.generatePoint(model, zoom, 0),
                    width: model.bounds.width + (GNodeViewBase.SELECTION_OFFSET * 2) / zoom,
                    height: model.bounds.height + (GNodeViewBase.SELECTION_OFFSET * 2) / zoom
                }
            }),
            ...[0, 1, 2, 3].map((pos) => this.renderSelectedOutlineCorner(model, zoom, pos))
        ];
        return result;
    }

    /**
     * Generates a corner of the selection rectangle.
     * Does not check if the element is selected.
     *
     * @param model The model of the element
     * @param zoom The zoom level of the diagram
     * @param pos The position of the corner
     * @returns The corner
     */
    private renderSelectedOutlineCorner(model: Readonly<GNode>, zoom: number, pos: number): VNode {
        const point = this.generatePoint(model, zoom, pos);
        return svg("rect", {
            class: {
                "stroke-primary": true,
                "stroke-[1.5px]": true,
                "fill-background": true,
                "non-scaling-stroke": true,
                "pointer-events-none": true
            },
            attrs: {
                x: point.x - GNodeViewBase.INNER_POINT_SIZE / 2 / zoom,
                y: point.y - GNodeViewBase.INNER_POINT_SIZE / 2 / zoom,
                width: GNodeViewBase.INNER_POINT_SIZE / zoom,
                height: GNodeViewBase.INNER_POINT_SIZE / zoom,
                rx: GNodeViewBase.INNER_POINT_SIZE / 4 / zoom
            }
        });
    }

    /**
     * Generates resize handles (edges and corners).
     * Does not check if the element is selected.
     *
     * @param model The model of the element
     * @param zoom The zoom level of the diagram
     * @returns The resize handles
     */
    private renderResizeHandles(model: Readonly<GNode>, zoom: number): VNode[] {
        return GNodeViewBase.RESIZE_HANDLES.map((config) => this.renderResizeHandle(model, zoom, config));
    }

    /**
     * Generates a single resize handle (edge or corner).
     *
     * @param model the node element to generate the resize handle for
     * @param zoom the zoom level of the diagram
     * @param config the resize handle configuration
     * @returns the generated resize handle line
     */
    private renderResizeHandle(model: Readonly<GNode>, zoom: number, config: ResizeHandleConfig): VNode {
        const start = this.generatePoint(model, zoom, config.startPos);
        const end = this.generatePoint(model, zoom, config.endPos);
        const isCorner = config.startPos === config.endPos;
        const cursorClass = this.getResizeCursorClass(config.location);

        return svg("line", {
            attrs: {
                x1: start.x,
                y1: start.y,
                x2: end.x,
                y2: end.y,
                "data-resize-handle-location": config.location
            },
            class: {
                "stroke-transparent": true,
                "stroke-[12px]": true,
                "non-scaling-stroke": true,
                "stroke-linecap-square": isCorner,
                [cursorClass]: true
            }
        });
    }

    /**
     * Converts a GLSP ResizeHandleLocation to the corresponding Tailwind cursor utility class.
     *
     * @param location the GLSP resize handle location
     * @returns the Tailwind cursor utility class
     */
    private getResizeCursorClass(location: string): string {
        switch (location) {
            case ResizeHandleLocation.TopLeft:
            case ResizeHandleLocation.BottomRight:
                return "cursor-nwse-resize";
            case ResizeHandleLocation.Top:
            case ResizeHandleLocation.Bottom:
                return "cursor-ns-resize";
            case ResizeHandleLocation.TopRight:
            case ResizeHandleLocation.BottomLeft:
                return "cursor-nesw-resize";
            case ResizeHandleLocation.Left:
            case ResizeHandleLocation.Right:
                return "cursor-ew-resize";
            default:
                return "cursor-default";
        }
    }

    /**
     * Computes the position of a point on the selection rectangle.
     * 0 is the top left corner, 1 is the top right corner, 2 is the bottom right corner and 3 is the bottom left corner.
     *
     * @param model the model for which the point should be computed
     * @param zoom the zoom level of the diagram
     * @param pos the position of the point, taken modulo 4
     * @returns the point
     */
    private generatePoint(model: Readonly<GNode>, zoom: number, pos: number): Point {
        pos = pos % 4;
        const scaledOffset = GNodeViewBase.SELECTION_OFFSET / zoom;
        const x = pos === 1 || pos === 2 ? model.bounds.width + scaledOffset : -scaledOffset;
        const y = pos < 2 ? -scaledOffset : model.bounds.height + scaledOffset;
        return { x, y };
    }

    /**
     * Renders a semitransparent border when the node has an edge-edit highlight
     * (e.g. when it is a reconnect or create-edge target).
     *
     * @param model The model of the element
     * @returns The border elements
     */
    private renderEdgeEditHighlightBorder(model: Readonly<GNode>): VNode[] {
        const { width, height } = model.bounds;

        return [
            svg("path", {
                class: {
                    "stroke-primary/50": true,
                    "stroke-[15px]": true,
                    "fill-none": true,
                    "non-scaling-stroke": true,
                    "pointer-events-none": true,
                    "[stroke-linejoin:round]": true
                },
                attrs: {
                    d: `M0 0H${width}V${height}H0Z`
                }
            })
        ];
    }

    /**
     * Renders an anchor cue point for edge-edit highlighting.
     *
     * @param model The node model
     * @param anchor The projected anchor on the node bounds
     * @returns The rendered anchor cue VNode
     */
    private renderEdgeEditHighlightAnchor(model: Readonly<GNode>, anchor: { side: string; value: number }): VNode {
        const point = this.anchorToPoint(model, anchor);
        return renderCreateEdgeEndpoint(point, findViewportZoom(model));
    }

    /**
     * Converts an anchor descriptor to a local point on the node outline.
     *
     * @param model The node model
     * @param anchor The anchor descriptor
     * @returns The local point on the node outline
     */
    private anchorToPoint(model: Readonly<GNode>, anchor: { side: string; value: number }): Point {
        const value = Math.max(0, Math.min(1, anchor.value));

        switch (anchor.side) {
            case "top":
                return { x: model.bounds.width * value, y: 0 };
            case "bottom":
                return { x: model.bounds.width * value, y: model.bounds.height };
            case "left":
                return { x: 0, y: model.bounds.height * value };
            case "right":
            default:
                return { x: model.bounds.width, y: model.bounds.height * value };
        }
    }
}
