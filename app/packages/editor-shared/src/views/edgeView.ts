import type { RenderingContext, IView, Point, Bounds } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { GEdge, EdgeReconnectData } from "../model/edge.js";
import type { VNode } from "snabbdom";
import { EdgeRouter, type RouteComputationResult } from "../features/edge-rourting/edgeRouter.js";
import type { AnchorSide } from "@mdeo/editor-protocol";
import { findViewportZoom } from "../base/findViewportZoom.js";

const { injectable, inject } = sharedImport("inversify");
const { svg, Point: PointUtil } = sharedImport("@eclipse-glsp/sprotty");
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
 * Position of an edge attachment.
 */
export enum EdgeAttachmentPosition {
    SOURCE_LEFT = "source-left",
    SOURCE_RIGHT = "source-right",
    TARGET_LEFT = "target-left",
    TARGET_RIGHT = "target-right",
    MIDDLE_LEFT = "middle-left",
    MIDDLE_RIGHT = "middle-right"
}

/**
 * Interface for edge attachment data.
 * Attachments are additional visual elements positioned relative to the edge.
 */
export interface EdgeAttachment {
    /**
     * The VNode to render for this attachment
     */
    vnode: VNode | undefined;
    /**
     * The bounds of the attachment element
     */
    bounds: Bounds;
    /**
     * The target position along the edge
     */
    position: EdgeAttachmentPosition;
}

/**
 * Abstract base view for rendering edge elements.
 * Provides a foundation for custom edge visualizations with support for markers.
 */
@injectable()
export abstract class GEdgeView implements IView {
    @inject(EdgeRouter) protected edgeRouter!: EdgeRouter;

    /**
     * Distance from the edge line to the first attachment (in pixels)
     */
    protected attachmentDistanceToLine = 2;

    /**
     * Distance from the source/target element to the first attachment (in pixels)
     */
    protected attachmentDistanceFromElement = 6;

    /**
     * Distance between stacked attachments (in pixels)
     */
    protected attachmentDistanceBetween = 2;

    /**
     * Size of the reconnect handle (in pixels at zoom 1.0)
     */
    protected static readonly RECONNECT_HANDLE_SIZE = 20;

    /**
     * Size of the inner reconnect handle (in pixels at zoom 1.0)
     */
    protected static readonly RECONNECT_HANDLE_INNER_SIZE = 10;

    /**
     * Renders the edge element with optional markers, visible path, invisible selection path, and additional content.
     *
     * @param model The edge model to render
     * @param context The rendering context
     * @returns The rendered VNode or undefined if rendering is not possible
     */
    render(model: Readonly<GEdge>, context: RenderingContext): VNode | undefined {
        const routeResult = this.edgeRouter.computeRoute(model);
        const route = routeResult.route;

        const sourceMarkerData = this.renderSourceMarker(model, context);
        const targetMarkerData = this.renderTargetMarker(model, context);

        const startStrokeOffset = sourceMarkerData?.strokeOffset ?? 0;
        const endStrokeOffset = targetMarkerData?.strokeOffset ?? 0;

        const adjustedRoute = this.adjustRouteForMarkers(route, startStrokeOffset, endStrokeOffset);

        const children: VNode[] = [];

        if (sourceMarkerData && route.length >= 2) {
            const sourceMarker = this.transformMarker(sourceMarkerData.marker, route[0], route[1], "source");
            children.push(sourceMarker);
        }

        if (targetMarkerData && route.length >= 2) {
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

        children.push(...this.renderAdditional(model, context, routeResult));

        const attachments = this.getEdgeAttachments(model, context);
        children.push(...this.renderAttachments(attachments, routeResult));

        if (isSelected(model) && route.length >= 2) {
            children.push(...this.renderReconnectHandles(model, route));
        }

        const rootClasses: Record<string, boolean> = {};
        if (!isSelected(model)) {
            rootClasses["cursor-pointer"] = true;
        }
        if (model.reconnectData) {
            rootClasses["pointer-events-none"] = true;
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
        _routeResult: RouteComputationResult
    ): VNode[] {
        return [];
    }

    /**
     * Gets edge attachments to be rendered along the edge.
     * Override this method to provide custom attachments like multiplicity labels or properties.
     *
     * @param _model The edge model
     * @param _context The rendering context
     * @returns An array of edge attachments
     */
    protected getEdgeAttachments(_model: Readonly<GEdge>, _context: RenderingContext): EdgeAttachment[] {
        return [];
    }

    /**
     * Renders edge attachments at their computed positions.
     *
     * @param attachments The attachments to render
     * @param routeResult The route data
     * @returns An array of VNodes for the attachments
     */
    private renderAttachments(attachments: EdgeAttachment[], routeResult: RouteComputationResult): VNode[] {
        const vnodes: VNode[] = [];

        const groupedAttachments = new Map<EdgeAttachmentPosition, EdgeAttachment[]>();
        for (const attachment of attachments) {
            const group = groupedAttachments.get(attachment.position) ?? [];
            group.push(attachment);
            groupedAttachments.set(attachment.position, group);
        }

        for (const [position, group] of groupedAttachments) {
            vnodes.push(...this.renderAttachmentGroup(group, position, routeResult));
        }

        return vnodes;
    }

    /**
     * Renders a group of attachments at a specific position.
     *
     * @param attachments The attachments in the group
     * @param position The position along the edge
     * @param routeResult The route data
     * @returns An array of VNodes for the attachment group
     */
    private renderAttachmentGroup(
        attachments: EdgeAttachment[],
        position: EdgeAttachmentPosition,
        routeResult: RouteComputationResult
    ): VNode[] {
        const route = routeResult.route;
        if (route.length < 2) {
            return [];
        }

        const anchorInfo = this.getAnchorInfo(position, routeResult);
        if (!anchorInfo) {
            return [];
        }

        const isHorizontal = Math.abs(anchorInfo.dx) > Math.abs(anchorInfo.dy);
        const isLeftSide = this.determineAttachmentSide(position);
        let anchorPoint = anchorInfo.anchorPoint;

        if (
            (position === EdgeAttachmentPosition.SOURCE_LEFT ||
                position === EdgeAttachmentPosition.SOURCE_RIGHT ||
                position === EdgeAttachmentPosition.TARGET_LEFT ||
                position === EdgeAttachmentPosition.TARGET_RIGHT) &&
            anchorInfo.anchorSide
        ) {
            const direction = this.getDirectionFromSide(anchorInfo.anchorSide);
            const distanceFromElement = this.attachmentDistanceFromElement;
            anchorPoint = {
                x: anchorPoint.x + direction.x * distanceFromElement,
                y: anchorPoint.y + direction.y * distanceFromElement
            };
        }

        return this.stackAttachments(attachments, anchorPoint, position, isHorizontal, isLeftSide, anchorInfo);
    }

    /**
     * Gets the anchor information for an attachment position.
     *
     * @param position The attachment position
     * @param routeResult The route data
     * @returns The anchor point, direction vector, and optional anchor side, or undefined if not computable
     */
    private getAnchorInfo(
        position: EdgeAttachmentPosition,
        routeResult: RouteComputationResult
    ): { anchorPoint: Point; dx: number; dy: number; anchorSide?: AnchorSide } | undefined {
        const route = routeResult.route;
        if (route.length < 2) {
            return undefined;
        }

        let anchorPoint: Point;
        let dx: number;
        let dy: number;
        let anchorSide: AnchorSide | undefined;

        if (position === EdgeAttachmentPosition.SOURCE_LEFT || position === EdgeAttachmentPosition.SOURCE_RIGHT) {
            anchorPoint = route[0];
            anchorSide = routeResult.sourceAnchor?.side;
            if (anchorSide) {
                const dir = this.getDirectionFromSide(anchorSide);
                dx = dir.x;
                dy = dir.y;
            } else {
                dx = route[1].x - route[0].x;
                dy = route[1].y - route[0].y;
            }
        } else if (
            position === EdgeAttachmentPosition.TARGET_LEFT ||
            position === EdgeAttachmentPosition.TARGET_RIGHT
        ) {
            anchorPoint = route[route.length - 1];
            anchorSide = routeResult.targetAnchor?.side;
            if (anchorSide) {
                const dir = this.getDirectionFromSide(anchorSide);
                dx = dir.x;
                dy = dir.y;
            } else {
                dx = route[route.length - 2].x - route[route.length - 1].x;
                dy = route[route.length - 2].y - route[route.length - 1].y;
            }
        } else if (
            position === EdgeAttachmentPosition.MIDDLE_LEFT ||
            position === EdgeAttachmentPosition.MIDDLE_RIGHT
        ) {
            const middleIndex = Math.floor(route.length / 2);
            anchorPoint = PointUtil.linear(route[middleIndex - 1], route[middleIndex], 0.5);
            const directionPoint = route[middleIndex];
            dx = directionPoint.x - anchorPoint.x;
            dy = directionPoint.y - anchorPoint.y;
        } else {
            return undefined;
        }

        return { anchorPoint, dx, dy, anchorSide };
    }

    /**
     * Gets the direction vector from an anchor side.
     *
     * @param side The anchor side
     * @returns The direction vector
     */
    private getDirectionFromSide(side: AnchorSide): Point {
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
     * Determines whether attachments should be on the left side.
     *
     * @param position The attachment position
     * @returns True if attachments should be on the left side
     */
    private determineAttachmentSide(position: EdgeAttachmentPosition): boolean {
        if (
            position === EdgeAttachmentPosition.MIDDLE_LEFT ||
            position === EdgeAttachmentPosition.SOURCE_LEFT ||
            position === EdgeAttachmentPosition.TARGET_LEFT
        ) {
            return true;
        }
        if (
            position === EdgeAttachmentPosition.MIDDLE_RIGHT ||
            position === EdgeAttachmentPosition.SOURCE_RIGHT ||
            position === EdgeAttachmentPosition.TARGET_RIGHT
        ) {
            return false;
        }
        return false;
    }

    /**
     * Stacks and renders attachments at the computed position.
     *
     * Attachments are always stacked vertically (along the Y axis).
     * When attached to vertical lines they are aligned with the line (same Y as the anchor),
     * when attached to horizontal lines they are aligned with the box (same X as the anchor).
     * For horizontal middle attachments, the group is vertically centered around the anchor.
     *
     * The stack direction is always outwards from the box or line, depending on the context.
     *
     * @param attachments The attachments to stack
     * @param anchorPoint The anchor point on the edge
     * @param position The logical attachment position (source/target/middle, left/right)
     * @param isHorizontal Whether the local edge segment is horizontal
     * @param isLeftSide Whether attachments are on the logical left side
     * @param anchorInfo Direction and side information for the anchor
     * @returns An array of VNodes for the stacked attachments
     */
    private stackAttachments(
        attachments: EdgeAttachment[],
        anchorPoint: Point,
        position: EdgeAttachmentPosition,
        isHorizontal: boolean,
        isLeftSide: boolean,
        anchorInfo: { dx: number; dy: number; anchorSide?: AnchorSide }
    ): VNode[] {
        const vnodes: VNode[] = [];
        if (attachments.length === 0) {
            return vnodes;
        }

        const sideSign = isLeftSide ? -1 : 1;

        const distanceToLine = this.attachmentDistanceToLine;

        const baseLineX = anchorPoint.x;
        const baseLineY = anchorPoint.y;

        const isMiddle =
            position === EdgeAttachmentPosition.MIDDLE_LEFT || position === EdgeAttachmentPosition.MIDDLE_RIGHT;
        const isMiddleHorizontal = isMiddle && isHorizontal;
        const isMiddleVertical = isMiddle && !isHorizontal;

        let verticalSign = 1;
        if (!isHorizontal && !isMiddleVertical) {
            if (Math.abs(anchorInfo.dy) > Math.abs(anchorInfo.dx)) {
                verticalSign = anchorInfo.dy >= 0 ? 1 : -1;
            }

            if (
                position === EdgeAttachmentPosition.SOURCE_LEFT ||
                position === EdgeAttachmentPosition.SOURCE_RIGHT ||
                position === EdgeAttachmentPosition.TARGET_LEFT ||
                position === EdgeAttachmentPosition.TARGET_RIGHT
            ) {
                if (anchorInfo.anchorSide === "top") {
                    verticalSign = -1;
                } else if (anchorInfo.anchorSide === "bottom") {
                    verticalSign = 1;
                }
            }
        }

        let totalHeight = 0;
        if (isMiddleVertical) {
            for (const attachment of attachments) {
                totalHeight += attachment.bounds.height;
            }
            totalHeight += (attachments.length - 1) * this.attachmentDistanceBetween;
        }

        let currentOffset = isMiddleVertical ? -totalHeight / 2 : 0;

        for (const attachment of attachments) {
            const width = attachment.bounds.width;
            const height = attachment.bounds.height;

            let centerX = baseLineX;
            let centerY = baseLineY;

            if (isHorizontal) {
                const verticalDir = isLeftSide ? -1 : 1;
                const baseY = baseLineY + verticalDir * distanceToLine;

                if (isMiddleHorizontal) {
                    centerX = baseLineX;
                } else if (Math.abs(anchorInfo.dx) > 0) {
                    const alongDir = anchorInfo.dx >= 0 ? 1 : -1;
                    centerX = baseLineX + alongDir * (width / 2);
                } else {
                    centerX = baseLineX;
                }

                const localOffset = currentOffset + height / 2;
                centerY = baseY + verticalDir * localOffset;
            } else {
                const baseX = baseLineX + sideSign * distanceToLine;
                centerX = baseX + sideSign * (width / 2);

                const localOffset = currentOffset + height / 2;
                if (isMiddleVertical) {
                    centerY = baseLineY + localOffset;
                } else {
                    centerY = baseLineY + verticalSign * localOffset;
                }
            }

            const finalX = centerX - width / 2;
            const finalY = centerY - height / 2;

            const transformedGroup = svg(
                "g",
                {
                    attrs: {
                        transform: `translate(${finalX}, ${finalY})`
                    }
                },
                attachment.vnode
            );

            vnodes.push(transformedGroup);

            currentOffset += height + this.attachmentDistanceBetween;
        }

        return vnodes;
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
                    "data-edge-segment-index": i
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

    /**
     * Renders reconnect handles at the source and target of the edge.
     * Only rendered when the edge is selected.
     *
     * @param model The edge model
     * @param route The computed route
     * @returns An array of VNodes for the reconnect handles
     */
    protected renderReconnectHandles(model: Readonly<GEdge>, route: Point[]): VNode[] {
        const reconnectData = (model as any).reconnectData as EdgeReconnectData | undefined;
        const zoom = findViewportZoom(model);
        const handles: VNode[] = [];

        const sourcePoint = route[0];
        const targetPoint = route[route.length - 1];

        const sourceActive = reconnectData?.end === "source";
        const targetActive = reconnectData?.end === "target";

        handles.push(this.renderReconnectHandle(sourcePoint, "source", zoom, sourceActive ?? false));
        handles.push(this.renderReconnectHandle(targetPoint, "target", zoom, targetActive ?? false));

        return handles;
    }

    /**
     * Renders a single reconnect handle.
     *
     * @param position The position of the handle
     * @param end The end type (source or target)
     * @param zoom The current zoom level
     * @param isActive Whether this handle is currently being dragged
     * @returns The VNode for the handle
     */
    protected renderReconnectHandle(position: Point, end: "source" | "target", zoom: number, isActive: boolean): VNode {
        const outerRadius = GEdgeView.RECONNECT_HANDLE_SIZE / 2 / zoom;
        const innerRadius = GEdgeView.RECONNECT_HANDLE_INNER_SIZE / 2 / zoom;

        return svg(
            "g",
            {},
            svg("circle", {
                attrs: {
                    cx: position.x,
                    cy: position.y,
                    r: outerRadius,
                    "data-reconnect-handle": end
                },
                class: {
                    "fill-transparent": !isActive,
                    "fill-primary/50": isActive,
                    "hover:fill-primary/50": true,
                    "non-scaling-stroke": true,
                    "cursor-pointer": true
                }
            }),
            svg("circle", {
                attrs: {
                    cx: position.x,
                    cy: position.y,
                    r: innerRadius,
                    "data-reconnect-handle": end
                },
                class: {
                    "fill-background": true,
                    "stroke-primary": true,
                    "stroke-[1.5px]": true,
                    "non-scaling-stroke": true,
                    "pointer-events-none": true
                }
            })
        );
    }
}
