import type { RenderingContext, IView, Point } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { GEdge } from "../model/edge.js";
import type { VNode } from "snabbdom";
import { EdgeRouter } from "../features/edge-rourting/edgeRouter.js";

const { injectable, inject } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");
const { isSelected } = sharedImport("@eclipse-glsp/client");

/**
 * Data returned by marker render methods.
 */
export interface EdgeMarkerData {
    /**
     * The VNode representing the marker
     */
    marker: VNode;
    /**
     * How much the path stroke start/end point should be moved away from the original point
     */
    strokeOffset: number;
    /**
     * How much elements (like labels) should be offset from the original point
     */
    elementOffset: number;
}

/**
 * Data about the edge route and marker offsets.
 */
export interface EdgeRouteData {
    /**
     * The computed route points
     */
    route: Point[];
    /**
     * Offset for elements at the start of the edge
     */
    startElementOffset: number;
    /**
     * Offset for elements at the end of the edge
     */
    endElementOffset: number;
}

/**
 * Abstract base view for rendering edge elements.
 * Provides a foundation for custom edge visualizations with support for markers.
 */
@injectable()
export abstract class GEdgeView implements IView {
    @inject(EdgeRouter) protected edgeRouter!: EdgeRouter;

    /**
     * Renders the edge element with optional markers, visible path, invisible selection path, and additional content.
     *
     * @param model The edge model to render
     * @param context The rendering context
     * @returns The rendered VNode or undefined if rendering is not possible
     */
    render(model: Readonly<GEdge>, context: RenderingContext): VNode | undefined {
        const route = this.edgeRouter.computeRoute(model).route;

        const sourceMarkerData = this.renderSourceMarker(model, context);
        const targetMarkerData = this.renderTargetMarker(model, context);

        const startStrokeOffset = sourceMarkerData?.strokeOffset ?? 0;
        const endStrokeOffset = targetMarkerData?.strokeOffset ?? 0;
        const startElementOffset = sourceMarkerData?.elementOffset ?? 0;
        const endElementOffset = targetMarkerData?.elementOffset ?? 0;

        const adjustedRoute = this.adjustRouteForMarkers(route, startStrokeOffset, endStrokeOffset);

        const routeData: EdgeRouteData = {
            route,
            startElementOffset,
            endElementOffset
        };

        const children: VNode[] = [];

        if (sourceMarkerData) {
            const sourceMarker = this.transformMarker(sourceMarkerData.marker, route[0], route[1], "source");
            children.push(sourceMarker);
        }

        if (targetMarkerData) {
            const targetMarker = this.transformMarker(
                targetMarkerData.marker,
                route[route.length - 1],
                route[route.length - 2],
                "target"
            );
            children.push(targetMarker);
        }

        children.push(this.renderVisiblePath(adjustedRoute, model, context));

        children.push(...this.renderInvisiblePath(adjustedRoute, model));

        children.push(...this.renderAdditional(model, context, routeData));
        children.push(...this.renderAdditional(model, context, routeData));

        // Apply cursor classes based on selection state
        const rootClasses: Record<string, boolean> = {};
        if (!isSelected(model)) {
            rootClasses["cursor-pointer"] = true;
        }

        return svg("g", { class: rootClasses }, ...children);
    }

    /**
     * Renders the source marker for the edge.
     * By default, returns undefined which indicates no marker.
     *
     * Markers should be rendered at origin (0,0) pointing right (towards positive x-axis).
     * The marker will be automatically transformed and rotated to align with the edge direction.
     *
     * @param _model The edge model
     * @param _context The rendering context
     * @returns Marker data or undefined if no marker should be rendered
     */
    protected renderSourceMarker(_model: Readonly<GEdge>, _context: RenderingContext): EdgeMarkerData | undefined {
        return undefined;
    }

    /**
     * Renders the target marker for the edge.
     * By default, returns undefined which indicates no marker.
     *
     * Markers should be rendered at origin (0,0) pointing right (towards positive x-axis).
     * The marker will be automatically transformed and rotated to align with the edge direction.
     *
     * @param _model The edge model
     * @param _context The rendering context
     * @returns Marker data or undefined if no marker should be rendered
     */
    protected renderTargetMarker(_model: Readonly<GEdge>, _context: RenderingContext): EdgeMarkerData | undefined {
        return undefined;
    }

    /**
     * Renders additional content for the edge (e.g., labels, decorations).
     * Override this method to add custom elements to the edge.
     *
     * @param _model The edge model
     * @param _context The rendering context
     * @param _routeData The route data including offsets
     * @returns An array of VNodes to render
     */
    protected renderAdditional(
        _model: Readonly<GEdge>,
        _context: RenderingContext,
        _routeData: EdgeRouteData
    ): VNode[] {
        return [];
    }

    /**
     * Adjusts the route points based on marker stroke offsets.
     *
     * @param route The original route points
     * @param startOffset The offset to apply at the start
     * @param endOffset The offset to apply at the end
     * @returns The adjusted route
     */
    private adjustRouteForMarkers(route: Point[], startOffset: number, endOffset: number): Point[] {
        if (route.length < 2) {
            return route;
        }

        const adjusted = [...route];

        if (startOffset > 0) {
            const dx = route[1].x - route[0].x;
            const dy = route[1].y - route[0].y;
            const len = Math.sqrt(dx * dx + dy * dy);
            if (len > 0) {
                adjusted[0] = {
                    x: route[0].x + (dx / len) * startOffset,
                    y: route[0].y + (dy / len) * startOffset
                };
            }
        }

        if (endOffset > 0) {
            const lastIdx = route.length - 1;
            const dx = route[lastIdx].x - route[lastIdx - 1].x;
            const dy = route[lastIdx].y - route[lastIdx - 1].y;
            const len = Math.sqrt(dx * dx + dy * dy);
            if (len > 0) {
                adjusted[lastIdx] = {
                    x: route[lastIdx].x - (dx / len) * endOffset,
                    y: route[lastIdx].y - (dy / len) * endOffset
                };
            }
        }

        return adjusted;
    }

    /**
     * Transforms and rotates a marker to align with the edge direction.
     *
     * @param marker The marker VNode
     * @param position The position point where the marker should be placed
     * @param directionPoint The point that determines the direction
     * @param type Whether this is a 'source' or 'target' marker
     * @returns The transformed marker VNode
     */
    private transformMarker(marker: VNode, position: Point, directionPoint: Point, type: "source" | "target"): VNode {
        const dx = position.x - directionPoint.x;
        const dy = position.y - directionPoint.y;
        const angle = Math.atan2(dy, dx) * (180 / Math.PI);

        const rotation = type === "source" ? angle + 180 : angle;

        return svg(
            "g",
            {
                attrs: {
                    transform: `translate(${position.x}, ${position.y}) rotate(${rotation})`
                }
            },
            marker
        );
    }

    /**
     * Renders the visible path of the edge.
     *
     * @param route The route points (adjusted for markers)
     * @param _model The edge model
     * @param _context The rendering context
     * @returns The path VNode
     */
    protected renderVisiblePath(route: Point[], _model: Readonly<GEdge>, _context: RenderingContext): VNode {
        const pathData = this.createPathData(route);

        return svg("path", {
            class: {
                "stroke-foreground": true,
                "fill-none": true,
                "stroke-[1.5px]": true
            },
            attrs: {
                d: pathData
            }
        });
    }

    /**
     * Renders the invisible selection path as individual line segments.
     * Uses non-scaling-stroke for consistent hit detection.
     * Adds data attributes for edge editing when the edge is selected.
     *
     * @param route The route points (adjusted for markers)
     * @param model The edge model
     * @returns An array of line segment VNodes
     */
    protected renderInvisiblePath(route: Point[], model: Readonly<GEdge>): VNode[] {
        const segments: VNode[] = [];
        const selected = isSelected(model);

        for (let i = 0; i < route.length - 1; i++) {
            const dx = route[i + 1].x - route[i].x;
            const dy = route[i + 1].y - route[i].y;
            const isHorizontal = Math.abs(dx) > Math.abs(dy);

            const segmentClasses: Record<string, boolean> = {
                "non-scaling-stroke": true,
                "stroke-transparent": true,
                "stroke-[10px]": true
            };

            if (selected) {
                segmentClasses[isHorizontal ? "cursor-ns-resize" : "cursor-ew-resize"] = true;
            }

            const segment = svg("line", {
                class: segmentClasses,
                attrs: {
                    x1: route[i].x,
                    y1: route[i].y,
                    x2: route[i + 1].x,
                    y2: route[i + 1].y,
                    "data-edge-segment-index": i,
                    "data-edge-id": model.id
                }
            });
            segments.push(segment);
        }

        return segments;
    }

    /**
     * Creates an SVG path data string from route points.
     *
     * @param points The route points
     * @returns The SVG path data string
     */
    private createPathData(points: Point[]): string {
        if (points.length === 0) {
            return "";
        }

        let path = `M ${points[0].x} ${points[0].y}`;
        for (let i = 1; i < points.length; i++) {
            path += ` L ${points[i].x} ${points[i].y}`;
        }
        return path;
    }
}
