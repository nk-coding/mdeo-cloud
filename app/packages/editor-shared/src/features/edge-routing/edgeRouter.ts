import type { Point } from "@eclipse-glsp/protocol";
import type { Bounds as BoundsType } from "@eclipse-glsp/protocol";
import type { EdgeLayoutMetadata, AnchorSide, EdgeAnchor } from "@mdeo/protocol-common";
import type { GEdge, EdgeReconnectData, EdgeCreateData } from "../../model/edge.js";
import type { BoundsAware, ISnapper } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { injectable, inject } = sharedImport("inversify");
const { almostEquals, Bounds, Point: PointUtil } = sharedImport("@eclipse-glsp/protocol");
const { TYPES, limit } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Result of the internal route computation.
 */
export interface RouteComputationResult {
    /**
     * The computed route as a list of points.
     * Includes anchor start and end points.
     */
    route: Point[];
    /**
     * The new routing metadata.
     */
    meta: EdgeLayoutMetadata;
    /**
     * The source anchor used for the route.
     * Always defined, even if entry in the meta is undefined.
     */
    sourceAnchor: EdgeAnchor;
    /**
     * The target anchor used for the route.
     * Always defined, even if entry in the meta is undefined.
     */
    targetAnchor: EdgeAnchor;
}

/**
 * Injectable edge router service that provides routing computation methods.
 * Extracted from GEdge to allow injection and reuse in views and tools.
 */
@injectable()
export class EdgeRouter {
    /**
     * The snapper service for snapping points.
     */
    @inject(TYPES.ISnapper) public snapper!: ISnapper;

    /**
     * Computes the route for an edge based on its metadata and bounding boxes.
     *
     * @param edge The edge to compute the route for
     * @returns The route computation result
     */
    computeRoute(edge: GEdge): RouteComputationResult {
        const reconnectData = edge.reconnectData as EdgeReconnectData | undefined;
        if (reconnectData) {
            return this.computeReconnectRoute(edge, reconnectData);
        }

        const createData = edge.edgeCreateData as EdgeCreateData | undefined;
        if (createData) {
            return this.computeCreateEdgeRoute(edge, createData);
        }

        const sourceBounds = (edge.index.getById(edge.sourceId) as BoundsAware | undefined)?.bounds ?? Bounds.ZERO;
        const targetBounds = (edge.index.getById(edge.targetId) as BoundsAware | undefined)?.bounds ?? Bounds.ZERO;
        const meta = edge.meta;

        const routingPoints = meta?.routingPoints ?? [];
        let sourceAnchor = meta?.sourceAnchor;
        let targetAnchor = meta?.targetAnchor;

        const cleanedRoutingPoints = this.removePointsInBounds(routingPoints, sourceBounds, targetBounds);

        let routeForAnchorComputation: Point[] = cleanedRoutingPoints;
        if (sourceAnchor != undefined && targetAnchor != undefined) {
            const startPoint = this.anchorToPoint(sourceAnchor, sourceBounds);
            const endPoint = this.anchorToPoint(targetAnchor, targetBounds);
            routeForAnchorComputation = this.normalizeRoutingPoints(
                startPoint,
                sourceAnchor.side,
                cleanedRoutingPoints,
                endPoint,
                targetAnchor.side
            );
            routeForAnchorComputation = this.removePointsInBounds(
                routeForAnchorComputation,
                sourceBounds,
                targetBounds
            );
        }

        const computedAnchors = this.computeAnchorsFromRoutingPoints(
            edge,
            routeForAnchorComputation,
            sourceBounds,
            targetBounds
        );
        if (sourceAnchor == undefined || targetAnchor == undefined) {
            sourceAnchor = computedAnchors.source.anchor;
            targetAnchor = computedAnchors.target.anchor;
        } else {
            const startPoint = this.anchorToPoint(sourceAnchor, sourceBounds);
            const endPoint = this.anchorToPoint(targetAnchor, targetBounds);
            if (!almostEquals(startPoint.x, endPoint.x) && !almostEquals(startPoint.y, endPoint.y)) {
                sourceAnchor = computedAnchors.source.anchor;
                targetAnchor = computedAnchors.target.anchor;
            }
        }

        const startPoint = this.anchorToPoint(sourceAnchor, sourceBounds);
        const endPoint = this.anchorToPoint(targetAnchor, targetBounds);

        const intermediatePoints = cleanedRoutingPoints.length > 0 ? [...cleanedRoutingPoints] : [];

        const normalizedPoints = this.normalizeRoutingPoints(
            startPoint,
            sourceAnchor.side,
            intermediatePoints,
            endPoint,
            targetAnchor.side
        );

        const route = this.simplifyRoutingPoints([startPoint, ...normalizedPoints, endPoint]);

        if (route.length >= 2) {
            sourceAnchor = this.cleanupAnchor(sourceAnchor, route[0], route[1]);
            targetAnchor = this.cleanupAnchor(targetAnchor, route.at(-1)!, route.at(-2)!);
        }

        const defineAnchorInMeta =
            meta?.sourceAnchor != undefined || meta?.targetAnchor != undefined || route.length > 3;

        return {
            route,
            meta: {
                routingPoints: this.routingPointsFromRoute(route),
                sourceAnchor: defineAnchorInMeta ? sourceAnchor : undefined,
                targetAnchor: defineAnchorInMeta ? targetAnchor : undefined
            },
            sourceAnchor,
            targetAnchor
        };
    }

    /**
     * Cleans up an anchor based on the edge orientation between two points.
     * If the anchor is on a side that does not match the edge orientation,
     * it is adjusted to be on the correct side with value 0 or 1.
     *
     * @param anchor the anchor to clean up
     * @param point1 the point at the anchor's end
     * @param point2 the next point in the route
     * @returns the cleaned up anchor
     */
    protected cleanupAnchor(anchor: EdgeAnchor, point1: Point, point2: Point): EdgeAnchor {
        if (anchor.value > 0 && anchor.value < 1) {
            return anchor;
        }
        const isVertical = Math.abs(point1.x - point2.x) < Math.abs(point1.y - point2.y);
        if (anchor.side === "top" || anchor.side === "bottom") {
            if (isVertical) {
                return anchor;
            } else {
                return {
                    side: anchor.value <= 0 ? "left" : "right",
                    value: anchor.side === "top" ? 0 : 1
                };
            }
        } else {
            if (!isVertical) {
                return anchor;
            } else {
                return {
                    side: anchor.value <= 0 ? "top" : "bottom",
                    value: anchor.side === "left" ? 0 : 1
                };
            }
        }
    }

    /**
     * Computes the route for an edge in reconnect mode.
     * Uses the reconnect position or anchor as one end and computes the route to the other end.
     *
     * @param edge The edge to compute the route for
     * @param reconnectData The reconnect data
     * @returns The route computation result
     */
    protected computeReconnectRoute(edge: GEdge, reconnectData: EdgeReconnectData): RouteComputationResult {
        const isReconnectingSource = reconnectData.end === "source";
        const fixedEndId = isReconnectingSource ? edge.targetId : edge.sourceId;
        const fixedEndBounds = (edge.index.getById(fixedEndId) as BoundsAware | undefined)?.bounds;

        if (fixedEndBounds == undefined) {
            throw new Error("Cannot compute reconnect route: fixed end bounds are undefined.");
        }

        let fixedAnchor: EdgeAnchor;
        let movingAnchor: EdgeAnchor;
        let fixedPoint: Point;
        let movingPoint: Point;

        if (reconnectData.anchor && reconnectData.targetId) {
            const targetBounds = (edge.index.getById(reconnectData.targetId) as BoundsAware | undefined)?.bounds;
            if (!targetBounds) {
                throw new Error("Cannot compute reconnect route: target bounds are undefined.");
            }

            movingAnchor = reconnectData.anchor;
            movingPoint = this.anchorToPoint(movingAnchor, targetBounds);

            const fixedProjection = this.projectAnchor(edge, movingPoint, fixedEndBounds);
            fixedAnchor = fixedProjection.anchor;
            fixedPoint = this.anchorToPoint(fixedAnchor, fixedEndBounds);
        } else if (reconnectData.position) {
            const fixedProjection = this.projectAnchor(edge, reconnectData.position, fixedEndBounds);
            fixedAnchor = fixedProjection.anchor;
            fixedPoint = this.anchorToPoint(fixedAnchor, fixedEndBounds);
            movingPoint = reconnectData.position;
            const fixedOrientation = Orientation.fromSide(fixedAnchor.side);
            if (fixedOrientation === "horizontal") {
                movingAnchor = { side: movingPoint.y < fixedPoint.y ? "bottom" : "top", value: 0.5 };
            } else {
                movingAnchor = { side: movingPoint.x < fixedPoint.x ? "right" : "left", value: 0.5 };
            }
        } else {
            throw new Error("Invalid reconnect data: must have either position or anchor");
        }

        const startPoint = isReconnectingSource ? movingPoint : fixedPoint;
        const startSide = isReconnectingSource ? movingAnchor.side : fixedAnchor.side;
        const endPoint = isReconnectingSource ? fixedPoint : movingPoint;
        const endSide = isReconnectingSource ? fixedAnchor.side : movingAnchor.side;

        let reconnectIntermediatePoints: Point[] = [];
        const reconnectStartOrientation = Orientation.fromSide(startSide);
        const reconnectEndOrientation = Orientation.fromSide(endSide);
        if (reconnectStartOrientation === reconnectEndOrientation) {
            if (reconnectStartOrientation === "horizontal") {
                const midX = (startPoint.x + endPoint.x) / 2;
                reconnectIntermediatePoints = [{ x: midX, y: startPoint.y }];
            } else {
                const midY = (startPoint.y + endPoint.y) / 2;
                reconnectIntermediatePoints = [{ x: startPoint.x, y: midY }];
            }
        }

        const normalizedPoints = this.normalizeRoutingPoints(
            startPoint,
            startSide,
            reconnectIntermediatePoints,
            endPoint,
            endSide
        );

        const route = this.simplifyRoutingPoints([startPoint, ...normalizedPoints, endPoint]);

        return {
            route,
            meta: {
                routingPoints: this.routingPointsFromRoute(route),
                sourceAnchor: isReconnectingSource ? movingAnchor : fixedAnchor,
                targetAnchor: isReconnectingSource ? fixedAnchor : movingAnchor
            },
            sourceAnchor: isReconnectingSource ? movingAnchor : fixedAnchor,
            targetAnchor: isReconnectingSource ? fixedAnchor : movingAnchor
        };
    }

    /**
     * Computes the route for an edge being created via the create-edge tool.
     * The source end is always fixed; the target end either follows the cursor position
     * or snaps to a target node's anchor.
     *
     * @param edge The feedback edge being created
     * @param createData The create-edge data with source, position, and optional target
     * @returns The route computation result
     */
    protected computeCreateEdgeRoute(edge: GEdge, createData: EdgeCreateData): RouteComputationResult {
        const sourceBounds = (edge.index.getById(createData.sourceId) as BoundsAware | undefined)?.bounds;
        if (sourceBounds == undefined) {
            throw new Error("Cannot compute create-edge route: source bounds are undefined.");
        }
        if (!createData.sourceAnchor) {
            throw new Error("Cannot compute create-edge route: source anchor is undefined.");
        }

        const sourceAnchor = createData.sourceAnchor;
        const sourcePoint = this.anchorToPoint(sourceAnchor, sourceBounds);
        let targetAnchor: EdgeAnchor;
        let targetPoint: Point;
        let intermediatePoints: Point[] = [];

        if (createData.anchor && createData.targetId) {
            const targetBounds = (edge.index.getById(createData.targetId) as BoundsAware | undefined)?.bounds;
            if (!targetBounds) {
                throw new Error("Cannot compute create-edge route: target bounds are undefined.");
            }

            targetAnchor = createData.anchor;
            targetPoint = this.anchorToPoint(targetAnchor, targetBounds);

            const sourceOrientation = Orientation.fromSide(sourceAnchor.side);
            const targetOrientation = Orientation.fromSide(targetAnchor.side);
            if (sourceOrientation === targetOrientation) {
                if (sourceOrientation === "horizontal") {
                    const midX = (sourcePoint.x + targetPoint.x) / 2;
                    intermediatePoints = [{ x: midX, y: sourcePoint.y }];
                } else {
                    const midY = (sourcePoint.y + targetPoint.y) / 2;
                    intermediatePoints = [{ x: sourcePoint.x, y: midY }];
                }
            }
        } else if (createData.position) {
            targetPoint = createData.position;
            const sourceOrientation = Orientation.fromSide(sourceAnchor.side);
            if (sourceOrientation === "horizontal") {
                targetAnchor = { side: targetPoint.y < sourcePoint.y ? "bottom" : "top", value: 0.5 };
            } else {
                targetAnchor = { side: targetPoint.x < sourcePoint.x ? "right" : "left", value: 0.5 };
            }
        } else {
            throw new Error("Invalid create-edge data: must have either position or anchor with targetId.");
        }

        const normalizedPoints = this.normalizeRoutingPoints(
            sourcePoint,
            sourceAnchor.side,
            intermediatePoints,
            targetPoint,
            targetAnchor.side
        );

        const route = this.simplifyRoutingPoints([sourcePoint, ...normalizedPoints, targetPoint]);

        return {
            route,
            meta: {
                routingPoints: this.routingPointsFromRoute(route),
                sourceAnchor,
                targetAnchor
            },
            sourceAnchor,
            targetAnchor
        };
    }

    /**
     * Computes anchors from routing points by finding the closest points on the bounding boxes.
     * When there are no routing points, uses bounding box overlap to determine anchor positions.
     *
     * @param edge The edge being routed
     * @param routingPoints The routing points
     * @param sourceBounds The source bounding box
     * @param targetBounds The target bounding box
     * @returns The computed source and target anchors
     */
    protected computeAnchorsFromRoutingPoints(
        edge: GEdge,
        routingPoints: Point[],
        sourceBounds: BoundsType,
        targetBounds: BoundsType
    ): { source: AnchorProjection; target: AnchorProjection } {
        if (routingPoints.length > 0) {
            const firstPoint = routingPoints[0];
            const lastPoint = routingPoints[routingPoints.length - 1];

            return {
                source: this.projectAnchor(edge, firstPoint, sourceBounds),
                target: this.projectAnchor(edge, lastPoint, targetBounds)
            };
        } else {
            return this.computeAnchorsFromOverlap(edge, sourceBounds, targetBounds);
        }
    }

    /**
     * Computes anchors based on the overlap of two bounding boxes.
     * Finds the longer overlap in each direction and uses the middle.
     *
     * @param edge The edge being routed
     * @param sourceBounds The source bounding box
     * @param targetBounds The target bounding box
     * @returns The computed source and target anchors
     */
    protected computeAnchorsFromOverlap(
        edge: GEdge,
        sourceBounds: BoundsType,
        targetBounds: BoundsType
    ): { source: AnchorProjection; target: AnchorProjection } {
        const xOverlapStart = Math.max(sourceBounds.x, targetBounds.x);
        const xOverlapEnd = Math.min(sourceBounds.x + sourceBounds.width, targetBounds.x + targetBounds.width);
        const xOverlap = Math.max(0, xOverlapEnd - xOverlapStart);

        const yOverlapStart = Math.max(sourceBounds.y, targetBounds.y);
        const yOverlapEnd = Math.min(sourceBounds.y + sourceBounds.height, targetBounds.y + targetBounds.height);
        const yOverlap = Math.max(0, yOverlapEnd - yOverlapStart);

        const sourceCenter = Bounds.center(sourceBounds);
        const targetCenter = Bounds.center(targetBounds);

        let sourceAnchor: EdgeAnchor;
        let targetAnchor: EdgeAnchor;

        if (
            sourceBounds.width === 0 ||
            sourceBounds.height === 0 ||
            targetBounds.width === 0 ||
            targetBounds.height === 0
        ) {
            return {
                source: this.projectAnchor(edge, sourceCenter, sourceBounds),
                target: this.projectAnchor(edge, targetCenter, targetBounds)
            };
        }

        if (xOverlap > yOverlap) {
            let xMiddle = (xOverlapStart + xOverlapEnd) / 2;
            const snapped = this.snapper.snap({ x: xMiddle, y: 0 }, edge);
            xMiddle = limit(snapped.x, { min: xOverlapStart, max: xOverlapEnd });
            const startValue = limit((xMiddle - sourceBounds.x) / sourceBounds.width, { min: 0, max: 1 });
            const targetValue = limit((xMiddle - targetBounds.x) / targetBounds.width, { min: 0, max: 1 });

            if (sourceCenter.y < targetCenter.y) {
                sourceAnchor = { side: "bottom", value: startValue };
                targetAnchor = { side: "top", value: targetValue };
            } else {
                sourceAnchor = { side: "top", value: startValue };
                targetAnchor = { side: "bottom", value: targetValue };
            }
        } else {
            let yMiddle = (yOverlapStart + yOverlapEnd) / 2;
            const snapped = this.snapper.snap({ x: 0, y: yMiddle }, edge);
            yMiddle = limit(snapped.y, { min: yOverlapStart, max: yOverlapEnd });
            const startValue = limit((yMiddle - sourceBounds.y) / sourceBounds.height, { min: 0, max: 1 });
            const targetValue = limit((yMiddle - targetBounds.y) / targetBounds.height, { min: 0, max: 1 });

            if (sourceCenter.x < targetCenter.x) {
                sourceAnchor = { side: "right", value: startValue };
                targetAnchor = { side: "left", value: targetValue };
            } else {
                sourceAnchor = { side: "left", value: startValue };
                targetAnchor = { side: "right", value: targetValue };
            }
        }

        return {
            source: { anchor: sourceAnchor, isDirect: true },
            target: { anchor: targetAnchor, isDirect: true }
        };
    }

    /**
     * Projects a point to the closest anchor on a bounding box and determines if
     * a direct (orthogonal) projection is possible.
     *
     * A direct projection means the point can be connected orthogonally to the box side
     * without requiring additional routing points.
     *
     * @param edge The edge being routed
     * @param point The reference point
     * @param bounds The bounding box
     * @param preferSide Optional preferred projection side ("horizontal" or "vertical") if not directly over the box
     * @returns An object containing the anchor and whether direct projection is possible
     */
    projectAnchor(
        edge: GEdge,
        point: Point,
        bounds: BoundsType,
        preferSide?: "horizontal" | "vertical"
    ): AnchorProjection {
        const left = bounds.x;
        const right = bounds.x + bounds.width;
        const top = bounds.y;
        const bottom = bounds.y + bounds.height;

        const overX = point.x >= left && point.x <= right;
        const overY = point.y >= top && point.y <= bottom;

        const dxLeft = Math.abs(point.x - left);
        const dxRight = Math.abs(point.x - right);
        const dyTop = Math.abs(point.y - top);
        const dyBottom = Math.abs(point.y - bottom);

        type Candidate = {
            side: AnchorSide;
            distance: number;
            snapCoord: Point;
        };

        const candidates: Candidate[] = [];

        if (overX) {
            candidates.push(
                { side: "top", distance: dyTop, snapCoord: { x: point.x, y: top } },
                { side: "bottom", distance: dyBottom, snapCoord: { x: point.x, y: bottom } }
            );
        }

        if (overY) {
            candidates.push(
                { side: "left", distance: dxLeft, snapCoord: { x: left, y: point.y } },
                { side: "right", distance: dxRight, snapCoord: { x: right, y: point.y } }
            );
        }

        let chosen: Candidate;
        let isDirect: boolean;

        if (candidates.length > 0) {
            chosen = candidates.reduce((a, b) => (a.distance < b.distance ? a : b));
            isDirect = true;
        } else {
            if (preferSide === "vertical") {
                chosen =
                    dxLeft < dxRight
                        ? { side: "left", distance: dxLeft, snapCoord: { x: left, y: point.y } }
                        : { side: "right", distance: dxRight, snapCoord: { x: right, y: point.y } };
            } else {
                chosen =
                    dyTop < dyBottom
                        ? { side: "top", distance: dyTop, snapCoord: { x: point.x, y: top } }
                        : { side: "bottom", distance: dyBottom, snapCoord: { x: point.x, y: bottom } };
            }
            isDirect = false;
        }

        const snapped = this.snapper.snap(chosen.snapCoord, edge);

        if (bounds.width === 0 || bounds.height === 0) {
            return {
                anchor: { side: chosen.side, value: 0.5 },
                isDirect
            };
        }

        let value: number;
        if (chosen.side === "top" || chosen.side === "bottom") {
            value = limit((snapped.x - bounds.x) / bounds.width, { min: 0, max: 1 });
        } else {
            value = limit((snapped.y - bounds.y) / bounds.height, { min: 0, max: 1 });
        }

        return {
            anchor: { side: chosen.side, value },
            isDirect
        };
    }

    /**
     * Converts an anchor to an actual point on the bounding box.
     * Anchor value ranges from 0 (left/top) to 1 (right/bottom).
     *
     * @param anchor The anchor specification
     * @param bounds The bounding box
     * @returns The actual point on the bounding box edge
     */
    anchorToPoint(anchor: EdgeAnchor, bounds: BoundsType): Point {
        switch (anchor.side) {
            case "top":
                return { x: bounds.x + anchor.value * bounds.width, y: bounds.y };
            case "bottom":
                return { x: bounds.x + anchor.value * bounds.width, y: bounds.y + bounds.height };
            case "left":
                return { x: bounds.x, y: bounds.y + anchor.value * bounds.height };
            case "right":
                return { x: bounds.x + bounds.width, y: bounds.y + anchor.value * bounds.height };
        }
    }

    /**
     * Computes the midpoint between two bounding box centers.
     *
     * @param edge The edge being routed
     * @param sourceBounds The source bounding box
     * @param targetBounds The target bounding box
     * @returns The midpoint
     */
    protected computeMidpoint(edge: GEdge, sourceBounds: BoundsType, targetBounds: BoundsType): Point {
        const sourceCenter = Bounds.center(sourceBounds);
        const targetCenter = Bounds.center(targetBounds);
        const midpoint = {
            x: (sourceCenter.x + targetCenter.x) / 2,
            y: (sourceCenter.y + targetCenter.y) / 2
        };
        return this.snapper.snap(midpoint, edge);
    }

    /**
     * Removes routing points from the start that are inside the source bounds
     * and from the end that are inside the target bounds.
     *
     * @param points The routing points
     * @param sourceBounds The source bounding box
     * @param targetBounds The target bounding box
     * @returns The filtered routing points
     */
    protected removePointsInBounds(points: Point[], sourceBounds: BoundsType, targetBounds: BoundsType): Point[] {
        let startIndex = 0;
        while (startIndex < points.length && this.isPointInBounds(points[startIndex], sourceBounds)) {
            startIndex++;
        }

        let endIndex = points.length - 1;
        while (endIndex >= startIndex && this.isPointInBounds(points[endIndex], targetBounds)) {
            endIndex--;
        }

        return points.slice(startIndex, endIndex + 1);
    }

    /**
     * Checks if a point is inside a bounding box.
     *
     * @param point The point to check
     * @param bounds The bounding box
     * @returns True if the point is inside the bounds
     */
    protected isPointInBounds(point: Point, bounds: BoundsType): boolean {
        return (
            point.x >= bounds.x &&
            point.x <= bounds.x + bounds.width &&
            point.y >= bounds.y &&
            point.y <= bounds.y + bounds.height
        );
    }

    /**
     * Normalizes routing points so that between consecutive points,
     * only one dimension (x or y) changes.
     *
     * @param startPoint The start anchor point
     * @param startSide The start anchor side
     * @param points The routing points to normalize
     * @param endPoint The end anchor point
     * @param endSide The end anchor side
     * @returns The normalized routing points
     */
    protected normalizeRoutingPoints(
        startPoint: Point,
        startSide: AnchorSide,
        points: Point[],
        endPoint: Point,
        endSide: AnchorSide
    ): Point[] {
        const result: Point[] = [];
        let lastPoint = startPoint;
        let lastDirection = Orientation.fromSide(startSide);

        for (let i = 0; i < points.length; i++) {
            const currentPoint = points[i];

            if (!almostEquals(currentPoint.x, lastPoint.x) && !almostEquals(currentPoint.y, lastPoint.y)) {
                if (lastDirection === "horizontal") {
                    result.push({ x: currentPoint.x, y: lastPoint.y });
                    lastDirection = "vertical";
                } else {
                    result.push({ x: lastPoint.x, y: currentPoint.y });
                    lastDirection = "horizontal";
                }
            }

            result.push(currentPoint);
            lastPoint = currentPoint;

            if (!almostEquals(currentPoint.x, lastPoint.x)) {
                lastDirection = "horizontal";
            } else if (!almostEquals(currentPoint.y, lastPoint.y)) {
                lastDirection = "vertical";
            }
        }

        const endDirection = Orientation.fromSide(endSide);

        if (!almostEquals(endPoint.x, lastPoint.x) && !almostEquals(endPoint.y, lastPoint.y)) {
            if (endDirection === "horizontal") {
                result.push({ x: lastPoint.x, y: endPoint.y });
            } else {
                result.push({ x: endPoint.x, y: lastPoint.y });
            }
        }

        return result;
    }

    /**
     * Simplifies routing points by removing intermediate points that lie on the same line.
     *
     * @param points The routing points to simplify
     * @returns The simplified routing points
     */
    simplifyRoutingPoints(points: Point[]): Point[] {
        if (points.length <= 1) {
            return points;
        }

        const result: Point[] = [points[0]];

        for (let i = 1; i < points.length - 1; i++) {
            const prev = points[i - 1];
            const curr = points[i];
            const next = points[i + 1];

            const dx1 = curr.x - prev.x;
            const dy1 = curr.y - prev.y;
            const dx2 = next.x - curr.x;
            const dy2 = next.y - curr.y;

            const onSameHorizontal = almostEquals(dy1, 0) && almostEquals(dy2, 0);
            const onSameVertical = almostEquals(dx1, 0) && almostEquals(dx2, 0);

            if (!(onSameHorizontal || onSameVertical)) {
                result.push(curr);
            }
        }

        result.push(points[points.length - 1]);
        return result;
    }

    /**
     * Constructs the routing points from a full route by removing the anchor points.
     *
     * @param route The full route including anchor points
     * @returns The routing points excluding anchor points
     */
    routingPointsFromRoute(route: Point[]): Point[] {
        if (route.length > 4) {
            return route.slice(2, -2);
        } else if (route.length === 4) {
            return [PointUtil.linear(route[1], route[2], 0.5)];
        } else {
            return [];
        }
    }
}

/**
 * Orientation type representing horizontal or vertical direction.
 */
export type Orientation = "horizontal" | "vertical";

/**
 * Namespace for orientation utility functions.
 */
export namespace Orientation {
    /**
     * Converts an AnchorSide to an Orientation.
     *
     * @param side The anchor side
     * @returns The corresponding orientation, horizontal for left/right, vertical for top/bottom
     */
    export function fromSide(side: AnchorSide): Orientation {
        if (side === "left" || side === "right") {
            return "horizontal";
        } else {
            return "vertical";
        }
    }

    /**
     * Inverts the given orientation.
     *
     * @param orientation The orientation to invert
     * @returns The inverted orientation
     */
    export function invert(orientation: Orientation): Orientation {
        return orientation === "horizontal" ? "vertical" : "horizontal";
    }
}

/**
 * Result of projecting a point to an anchor, including whether direct projection is possible.
 */
export interface AnchorProjection {
    /**
     * The projected anchor.
     */
    anchor: EdgeAnchor;
    /**
     * Whether the projection is direct (orthogonal) to the bounding box side.
     */
    isDirect: boolean;
}
