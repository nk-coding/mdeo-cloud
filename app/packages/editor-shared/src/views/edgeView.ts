import type { RenderingContext, IView, Point, Bounds } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { GEdge, EdgeReconnectData } from "../model/edge.js";
import type { VNode } from "snabbdom";
import { EdgeRouter, type RouteComputationResult } from "../features/edge-routing/edgeRouter.js";
import type { AnchorSide } from "@mdeo/protocol-common";
import { findViewportZoom } from "../base/findViewportZoom.js";
import type { ContextActionRailOrientation } from "../features/context-actions/contextActions.js";
import { isIssueMarker, ISSUE_MARKER_SIZE } from "../features/decoration/issueMarker.js";
import { ToolStateManager } from "../features/tool-state/toolStateManager.js";

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
 *
 * SOURCE_LEFT, SOURCE_RIGHT, TARGET_LEFT, TARGET_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT are for text/label
 * attachments (properties, multiplicity, etc.) and use a dedicated stacking algorithm.
 * START, END, MIDDLE are used exclusively by context action rails with a separate placement algorithm.
 */
export enum EdgeAttachmentPosition {
    /**
     * Label attachment at the source end, on the left/above side of the edge line
     */
    SOURCE_LEFT = "source-left",
    /**
     * Label attachment at the source end, on the right/below side of the edge line
     */
    SOURCE_RIGHT = "source-right",
    /**
     * Label attachment at the target end, on the left/above side of the edge line
     */
    TARGET_LEFT = "target-left",
    /**
     * Label attachment at the target end, on the right/below side of the edge line
     */
    TARGET_RIGHT = "target-right",
    /**
     * Label attachment at the edge midpoint, on the left/above side of the edge line
     */
    MIDDLE_LEFT = "middle-left",
    /**
     * Label attachment at the edge midpoint, on the right/below side of the edge line
     */
    MIDDLE_RIGHT = "middle-right",
    /**
     * Context action rail at the start/source endpoint of the edge
     */
    START = "start",
    /**
     * Context action rail at the end/target endpoint of the edge
     */
    END = "end",
    /**
     * Context action rail at the midpoint of the edge
     */
    MIDDLE = "middle"
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
     * Optional renderer callback for attachments that need the final absolute position.
     */
    renderAt?: (position: Point, orientation: ContextActionRailOrientation) => VNode | undefined;
    /**
     * Layout orientation used when renderAt is provided.
     */
    orientation?: ContextActionRailOrientation;
    /**
     * The bounds of the attachment element
     */
    bounds: Bounds;
    /**
     * The target position along the edge
     */
    position: EdgeAttachmentPosition;
    /**
     * When true, this attachment is placed on the opposite side of the midpoint
     * from where visual MIDDLE attachments go. Used by context action rails at MIDDLE.
     */
    oppositeMiddleSide?: boolean;
}

/**
 * Abstract base view for rendering edge elements.
 * Provides a foundation for custom edge visualizations with support for markers.
 */
@injectable()
export abstract class GEdgeView implements IView {
    @inject(EdgeRouter) protected edgeRouter!: EdgeRouter;

    /**
     * Tool state manager used to suppress the reconnect handles while a creation
     * tool is active, keeping the canvas visually clean.
     */
    @inject(ToolStateManager) protected toolStateManager!: ToolStateManager;

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
    static readonly RECONNECT_HANDLE_SIZE = 20;

    /**
     * Size of the inner reconnect handle (in pixels at zoom 1.0)
     */
    static readonly RECONNECT_HANDLE_INNER_SIZE = 10;

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
        const sourceElementOffset = sourceMarkerData?.elementOffset ?? 0;
        const targetElementOffset = targetMarkerData?.elementOffset ?? 0;
        children.push(...this.renderAttachments(attachments, routeResult, sourceElementOffset, targetElementOffset));

        if (isSelected(model) && route.length >= 2 && !this.toolStateManager.isCreationMode()) {
            children.push(...this.renderReconnectHandles(model, route));
        }

        if (model.edgeCreateData && route.length >= 2) {
            children.push(...this.renderCreateEdgeEndpoints(model, route));
        }

        children.push(...this.renderIssueMarkers(model, route, context));

        const rootClasses: Record<string, boolean> = {};
        if (!isSelected(model)) {
            rootClasses["cursor-pointer"] = true;
        }
        if (model.reconnectData != undefined || model.edgeCreateData != undefined) {
            rootClasses["pointer-events-none"] = true;
            rootClasses["[&_*]:pointer-events-none!"] = true;
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
     * Renders issue marker badges for all GIssueMarker children of the edge.
     *
     * The badge is placed at the geometric midpoint of the middle route segment so
     * it stays visually associated with the edge regardless of its length or orientation.
     * The badge size remains constant in physical CSS pixels regardless of zoom level.
     *
     * @param model   The edge model whose children are searched for GIssueMarker elements.
     * @param route   The computed route points (must have at least two points).
     * @param context The current rendering context used to render the marker view.
     * @returns An array of SVG VNodes for the issue marker badges (empty when there are none).
     */
    private renderIssueMarkers(model: Readonly<GEdge>, route: Point[], context: RenderingContext): VNode[] {
        if (route.length < 2) {
            return [];
        }

        const markers = model.children.filter(isIssueMarker);
        if (markers.length === 0) {
            return [];
        }

        const zoom = findViewportZoom(model);
        const iconSvgSize = ISSUE_MARKER_SIZE / zoom;

        const markerVNode = context.renderElement(markers[0]);
        if (markerVNode == undefined) {
            return [];
        }

        // Place the badge at the midpoint of the middle route segment.
        const midIndex = Math.floor(route.length / 2);
        const midPoint = PointUtil.linear(route[midIndex - 1], route[midIndex], 0.5);

        const x = midPoint.x - iconSvgSize / 2;
        const y = midPoint.y - iconSvgSize / 2;

        return [svg("g", { attrs: { transform: `translate(${x}, ${y})` } }, markerVNode)];
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
     * @param sourceElementOffset Offset from the source element due to markers
     * @param targetElementOffset Offset from the target element due to markers
     * @returns An array of VNodes for the attachments
     */
    private renderAttachments(
        attachments: EdgeAttachment[],
        routeResult: RouteComputationResult,
        sourceElementOffset: number,
        targetElementOffset: number
    ): VNode[] {
        const vnodes: VNode[] = [];

        const groupedAttachments = new Map<EdgeAttachmentPosition, EdgeAttachment[]>();
        for (const attachment of attachments) {
            const group = groupedAttachments.get(attachment.position) ?? [];
            group.push(attachment);
            groupedAttachments.set(attachment.position, group);
        }

        for (const [position, group] of groupedAttachments) {
            vnodes.push(
                ...this.renderAttachmentGroup(group, position, routeResult, sourceElementOffset, targetElementOffset)
            );
        }

        return vnodes;
    }

    /**
     * Renders a group of attachments at a specific position.
     *
     * For SOURCE_LEFT/RIGHT and TARGET_LEFT/RIGHT (label positions at endpoints), applies
     * `attachmentDistanceFromElement` offset before stacking with the label stacking algorithm.
     * For MIDDLE_LEFT/RIGHT, uses the label stacking algorithm without element offset.
     * For START/END/MIDDLE (context rail positions), uses the rail stacking algorithm with no
     * distanceFromElement applied for START/END.
     *
     * @param attachments The attachments in the group
     * @param position The position along the edge
     * @param routeResult The route data
     * @param sourceElementOffset Offset from the source element due to markers
     * @param targetElementOffset Offset from the target element due to markers
     * @returns An array of VNodes for the attachment group
     */
    private renderAttachmentGroup(
        attachments: EdgeAttachment[],
        position: EdgeAttachmentPosition,
        routeResult: RouteComputationResult,
        sourceElementOffset: number,
        targetElementOffset: number
    ): VNode[] {
        const route = routeResult.route;
        if (route.length < 2) {
            return [];
        }

        const anchorInfo = this.getAnchorInfo(position, routeResult);
        if (!anchorInfo) {
            return [];
        }

        let anchorPoint = anchorInfo.anchorPoint;

        if (GEdgeView.isLabelPosition(position)) {
            // For SOURCE_*/TARGET_* label positions, offset away from the element before stacking
            if (
                (GEdgeView.isSourceLabelPosition(position) || GEdgeView.isTargetLabelPosition(position)) &&
                anchorInfo.anchorSide
            ) {
                const direction = this.getDirectionFromSide(anchorInfo.anchorSide);
                const distanceFromElement = this.attachmentDistanceFromElement;
                const markerOffset = GEdgeView.isSourceLabelPosition(position)
                    ? sourceElementOffset
                    : targetElementOffset;
                const totalOffset = distanceFromElement + markerOffset;
                anchorPoint = {
                    x: anchorPoint.x + direction.x * totalOffset,
                    y: anchorPoint.y + direction.y * totalOffset
                };
            }
            return this.stackLabelAttachments(attachments, anchorPoint, position, anchorInfo);
        }

        // Context rail positions (START, END, MIDDLE): no distanceFromElement for START/END
        return this.stackAttachments(attachments, anchorPoint, position, anchorInfo);
    }

    /**
     * Gets the anchor information for an attachment position.
     *
     * For START, returns the source point and direction from source toward the next route point.
     * For END, returns the target point and direction from target toward the previous route point.
     * For MIDDLE, returns the midpoint and direction along the middle segment.
     *
     * @param position The attachment position (START, END, or MIDDLE)
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

        // Normalize label positions to their equivalent base positions for anchor computation
        let basePosition: EdgeAttachmentPosition = position;
        if (position === EdgeAttachmentPosition.SOURCE_LEFT || position === EdgeAttachmentPosition.SOURCE_RIGHT) {
            basePosition = EdgeAttachmentPosition.START;
        } else if (
            position === EdgeAttachmentPosition.TARGET_LEFT ||
            position === EdgeAttachmentPosition.TARGET_RIGHT
        ) {
            basePosition = EdgeAttachmentPosition.END;
        } else if (
            position === EdgeAttachmentPosition.MIDDLE_LEFT ||
            position === EdgeAttachmentPosition.MIDDLE_RIGHT
        ) {
            basePosition = EdgeAttachmentPosition.MIDDLE;
        }

        let anchorPoint: Point;
        let dx: number;
        let dy: number;
        let anchorSide: AnchorSide | undefined;

        if (basePosition === EdgeAttachmentPosition.START) {
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
        } else if (basePosition === EdgeAttachmentPosition.END) {
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
        } else if (basePosition === EdgeAttachmentPosition.MIDDLE) {
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
     * Returns true if the position is a label attachment position
     * (SOURCE_LEFT/RIGHT, TARGET_LEFT/RIGHT, MIDDLE_LEFT/RIGHT).
     *
     * @param position The attachment position to check
     * @returns true if this is a label position
     */
    private static isLabelPosition(position: EdgeAttachmentPosition): boolean {
        return (
            position === EdgeAttachmentPosition.SOURCE_LEFT ||
            position === EdgeAttachmentPosition.SOURCE_RIGHT ||
            position === EdgeAttachmentPosition.TARGET_LEFT ||
            position === EdgeAttachmentPosition.TARGET_RIGHT ||
            position === EdgeAttachmentPosition.MIDDLE_LEFT ||
            position === EdgeAttachmentPosition.MIDDLE_RIGHT
        );
    }

    /**
     * Returns true if the position is a source-end label position (SOURCE_LEFT or SOURCE_RIGHT).
     *
     * @param position The attachment position to check
     * @returns true if this is a source label position
     */
    private static isSourceLabelPosition(position: EdgeAttachmentPosition): boolean {
        return position === EdgeAttachmentPosition.SOURCE_LEFT || position === EdgeAttachmentPosition.SOURCE_RIGHT;
    }

    /**
     * Returns true if the position is a target-end label position (TARGET_LEFT or TARGET_RIGHT).
     *
     * @param position The attachment position to check
     * @returns true if this is a target label position
     */
    private static isTargetLabelPosition(position: EdgeAttachmentPosition): boolean {
        return position === EdgeAttachmentPosition.TARGET_LEFT || position === EdgeAttachmentPosition.TARGET_RIGHT;
    }

    /**
     * Determines whether the attachment position is on the left or above side of the edge.
     *
     * Left/above side: SOURCE_LEFT, TARGET_LEFT, MIDDLE_LEFT.
     * Right/below side: SOURCE_RIGHT, TARGET_RIGHT, MIDDLE_RIGHT.
     *
     * @param position A label attachment position
     * @returns true if the attachment should be placed on the left/above side
     */
    private static determineAttachmentSide(position: EdgeAttachmentPosition): boolean {
        return (
            position === EdgeAttachmentPosition.SOURCE_LEFT ||
            position === EdgeAttachmentPosition.TARGET_LEFT ||
            position === EdgeAttachmentPosition.MIDDLE_LEFT
        );
    }

    /**
     * Stacks and renders label attachments (SOURCE_LEFT/RIGHT, TARGET_LEFT/RIGHT, MIDDLE_LEFT/RIGHT)
     * at the computed anchor position.
     *
     * For horizontal edges: attachments are placed above (LEFT) or below (RIGHT) the edge, stacking
     * outward, and offset horizontally along the edge direction.
     * For vertical edges: attachments are placed to the left (LEFT) or right (RIGHT) of the edge,
     * stacking outward, and offset vertically in the edge direction.
     * For MIDDLE_LEFT/RIGHT on vertical edges: attachments are centered vertically around the anchor.
     *
     * @param attachments The attachments to stack
     * @param anchorPoint The anchor point on the edge (already offset from element for SOURCE/TARGET)
     * @param position The label attachment position
     * @param anchorInfo Direction and side information for the anchor
     * @returns An array of VNodes for the stacked label attachments
     */
    private stackLabelAttachments(
        attachments: EdgeAttachment[],
        anchorPoint: Point,
        position: EdgeAttachmentPosition,
        anchorInfo: { dx: number; dy: number; anchorSide?: AnchorSide }
    ): VNode[] {
        const vnodes: VNode[] = [];
        if (attachments.length === 0) {
            return vnodes;
        }

        const isHorizontal = Math.abs(anchorInfo.dx) > Math.abs(anchorInfo.dy);
        const isLeftSide = GEdgeView.determineAttachmentSide(position);
        const sideSign = isLeftSide ? -1 : 1;
        const distanceToLine = this.attachmentDistanceToLine;
        const isMiddle =
            position === EdgeAttachmentPosition.MIDDLE_LEFT || position === EdgeAttachmentPosition.MIDDLE_RIGHT;
        const isMiddleHorizontal = isMiddle && isHorizontal;
        const isMiddleVertical = isMiddle && !isHorizontal;

        // For non-horizontal non-middle-vertical edges, determine vertical stacking direction
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

        // For vertical middle: center the entire stack vertically around the anchor
        let totalHeight = 0;
        if (isMiddleVertical) {
            for (const a of attachments) {
                totalHeight += a.bounds.height;
            }
            totalHeight += (attachments.length - 1) * this.attachmentDistanceBetween;
        }

        let currentOffset = isMiddleVertical ? -totalHeight / 2 : 0;

        for (const attachment of attachments) {
            const width = attachment.bounds.width;
            const height = attachment.bounds.height;
            let centerX: number;
            let centerY: number;

            if (isHorizontal) {
                const verticalDir = isLeftSide ? -1 : 1;
                const baseY = anchorPoint.y + verticalDir * distanceToLine;
                if (isMiddleHorizontal) {
                    centerX = anchorPoint.x;
                } else if (Math.abs(anchorInfo.dx) > 0) {
                    const alongDir = anchorInfo.dx >= 0 ? 1 : -1;
                    centerX = anchorPoint.x + alongDir * (width / 2);
                } else {
                    centerX = anchorPoint.x;
                }
                centerY = baseY + verticalDir * (currentOffset + height / 2);
            } else {
                // Vertical edge
                const baseX = anchorPoint.x + sideSign * distanceToLine;
                centerX = baseX + sideSign * (width / 2);
                if (isMiddleVertical) {
                    centerY = anchorPoint.y + (currentOffset + height / 2);
                } else {
                    centerY = anchorPoint.y + verticalSign * (currentOffset + height / 2);
                }
            }

            const finalX = centerX - width / 2;
            const finalY = centerY - height / 2;

            vnodes.push(
                svg(
                    "g",
                    {
                        attrs: {
                            transform: `translate(${finalX}, ${finalY})`
                        }
                    },
                    attachment.vnode
                )
            );

            currentOffset += height + this.attachmentDistanceBetween;
        }

        return vnodes;
    }

    /**
     * Stacks and renders context action rail attachments (START, END, MIDDLE) at the computed position.
     *
     * For START/END: the rail is placed in the direction opposite to where the edge is travelling,
     * centered on the edge axis. No distanceFromElement offset is applied — the raw route endpoint is used.
     * For MIDDLE: visual attachments go on one side (left/above by default),
     * context action rails (oppositeMiddleSide=true) go on the opposite side.
     *
     * @param attachments The attachments to stack
     * @param anchorPoint The anchor point on the edge
     * @param position The context rail position (START, END, or MIDDLE)
     * @param anchorInfo Direction and side information for the anchor
     * @returns An array of VNodes for the stacked attachments
     */
    private stackAttachments(
        attachments: EdgeAttachment[],
        anchorPoint: Point,
        position: EdgeAttachmentPosition,
        anchorInfo: { dx: number; dy: number; anchorSide?: AnchorSide }
    ): VNode[] {
        const vnodes: VNode[] = [];
        if (attachments.length === 0) {
            return vnodes;
        }

        const isEdgeHorizontal = Math.abs(anchorInfo.dx) > Math.abs(anchorInfo.dy);
        const distanceToLine = this.attachmentDistanceToLine;
        const isMiddle = position === EdgeAttachmentPosition.MIDDLE;

        const edgeDirSign = isEdgeHorizontal ? (anchorInfo.dx >= 0 ? 1 : -1) : anchorInfo.dy >= 0 ? 1 : -1;

        for (const attachment of attachments) {
            const width = attachment.bounds.width;
            const height = attachment.bounds.height;

            let centerX: number;
            let centerY: number;

            if (isMiddle) {
                const defaultSideSign = -1;
                const sideSign = attachment.oppositeMiddleSide ? -defaultSideSign : defaultSideSign;

                if (isEdgeHorizontal) {
                    centerX = anchorPoint.x;
                    centerY = anchorPoint.y + sideSign * (distanceToLine + height / 2);
                } else {
                    centerX = anchorPoint.x + sideSign * (distanceToLine + width / 2);
                    centerY = anchorPoint.y;
                }
            } else {
                const oppositeSign = -edgeDirSign;

                if (isEdgeHorizontal) {
                    centerX = anchorPoint.x + oppositeSign * (distanceToLine + width / 2);
                    centerY = anchorPoint.y;
                } else {
                    centerX = anchorPoint.x;
                    centerY = anchorPoint.y + oppositeSign * (distanceToLine + height / 2);
                }
            }

            const finalX = centerX - width / 2;
            const finalY = centerY - height / 2;

            if (attachment.renderAt != undefined) {
                const rendered = attachment.renderAt(
                    { x: finalX, y: finalY },
                    attachment.orientation ?? (isEdgeHorizontal ? "horizontal" : "vertical")
                );
                if (rendered != undefined) {
                    vnodes.push(rendered);
                }
            } else {
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
            }
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
    protected createPathData(points: Point[]): string {
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
        const reconnectData = model.reconnectData as EdgeReconnectData | undefined;
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
     * Renders non-interactive endpoint indicators for create-edge feedback.
     * Both source and target endpoints stay visible during phase 2.
     *
     * @param model The edge model
     * @param route The computed route
     * @returns Endpoint indicator VNodes
     */
    protected renderCreateEdgeEndpoints(model: Readonly<GEdge>, route: Point[]): VNode[] {
        const zoom = findViewportZoom(model);
        const sourcePoint = route[0];
        const targetPoint = route[route.length - 1];

        return [renderCreateEdgeEndpoint(sourcePoint, zoom), renderCreateEdgeEndpoint(targetPoint, zoom)];
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

/**
 * Renders a single non-interactive endpoint indicator.
 *
 * @param position Endpoint position
 * @param zoom Current zoom level
 * @returns The endpoint indicator VNode
 */
export function renderCreateEdgeEndpoint(position: Point, zoom: number): VNode {
    const outerRadius = GEdgeView.RECONNECT_HANDLE_SIZE / 2 / zoom;
    const innerRadius = GEdgeView.RECONNECT_HANDLE_INNER_SIZE / 2 / zoom;

    return svg(
        "g",
        {},
        svg("circle", {
            attrs: {
                cx: position.x,
                cy: position.y,
                r: outerRadius
            },
            class: {
                "fill-primary/25": true,
                "non-scaling-stroke": true,
                "pointer-events-none": true
            }
        }),
        svg("circle", {
            attrs: {
                cx: position.x,
                cy: position.y,
                r: innerRadius
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
