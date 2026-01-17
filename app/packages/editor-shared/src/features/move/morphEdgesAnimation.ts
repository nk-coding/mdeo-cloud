import type { EdgeAnchor } from "@mdeo/editor-protocol";
import type { EdgeMemento } from "./edgeMemento.js";
import type { EdgeRouter } from "../edge-rourting/edgeRouter.js";
import type { CommandExecutionContext, GModelRoot, Point } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { Animation } = sharedImport("@eclipse-glsp/sprotty");
const { Point: PointUtil } = sharedImport("@eclipse-glsp/protocol");

/**
 * Expanded route for animation with normalized points.
 */
interface ExpandedEdgeMorph {
    startExpandedRoute: Point[];
    endExpandedRoute: Point[];
    memento: EdgeMemento;
}

/**
 * Animation for morphing edge routes.
 * Ensures horizontal segments stay horizontal and vertical segments stay vertical.
 */
export class MorphEdgesAnimation extends Animation {
    protected expanded: ExpandedEdgeMorph[] = [];

    constructor(
        protected model: GModelRoot,
        originalMementi: EdgeMemento[],
        context: CommandExecutionContext,
        protected edgeRouter: EdgeRouter,
        protected reverse: boolean = false
    ) {
        super(context);

        originalMementi.forEach((edgeMemento) => {
            const start = this.reverse ? edgeMemento.after : edgeMemento.before;
            const end = this.reverse ? edgeMemento.before : edgeMemento.after;

            const startRoute = start.route;
            const endRoute = end.route;

            const normalized = this.normalizeRoutes(startRoute, endRoute);

            this.expanded.push({
                startExpandedRoute: normalized.start,
                endExpandedRoute: normalized.end,
                memento: edgeMemento
            });
        });
    }

    /**
     * Normalizes two routes to have the same length and matching segment orientations.
     * This ensures horizontal segments morph to horizontal and vertical to vertical.
     */
    protected normalizeRoutes(startRoute: Point[], endRoute: Point[]): { start: Point[]; end: Point[] } {
        if (startRoute.length === 0 || endRoute.length === 0) {
            return { start: startRoute, end: endRoute };
        }

        //TODO find out how this is supposed to work
        // const startInner = startRoute.slice(1, -1);
        // const endInner = endRoute.slice(1, -1);

        const startOrientations = this.getSegmentOrientations(startRoute);
        const endOrientations = this.getSegmentOrientations(endRoute);

        const maxLength = Math.max(startOrientations.length, endOrientations.length);

        const normalizedStart = this.expandRouteToLength(startRoute, startOrientations, maxLength);
        const normalizedEnd = this.expandRouteToLength(endRoute, endOrientations, maxLength);

        return { start: normalizedStart, end: normalizedEnd };
    }

    /**
     * Gets the orientation (horizontal/vertical) of each segment in a route.
     */
    protected getSegmentOrientations(route: Point[]): ("horizontal" | "vertical")[] {
        const orientations: ("horizontal" | "vertical")[] = [];
        for (let i = 0; i < route.length - 1; i++) {
            const p1 = route[i];
            const p2 = route[i + 1];
            const dx = Math.abs(p2.x - p1.x);
            const dy = Math.abs(p2.y - p1.y);
            orientations.push(dx > dy ? "horizontal" : "vertical");
        }
        return orientations;
    }

    /**
     * Expands a route to a target length by duplicating points.
     */
    protected expandRouteToLength(
        route: Point[],
        orientations: ("horizontal" | "vertical")[],
        targetLength: number
    ): Point[] {
        if (orientations.length >= targetLength) {
            return route;
        }

        const result = [...route];
        while (result.length - 1 < targetLength) {
            let longestIdx = 0;
            let longestDist = 0;
            for (let i = 0; i < result.length - 1; i++) {
                const dist = PointUtil.euclideanDistance(result[i], result[i + 1]);
                if (dist > longestDist) {
                    longestDist = dist;
                    longestIdx = i;
                }
            }

            const midpoint = PointUtil.linear(result[longestIdx], result[longestIdx + 1], 0.5);
            result.splice(longestIdx + 1, 0, midpoint);
        }

        return result;
    }

    tween(t: number): GModelRoot {
        if (t === 1) {
            this.expanded.forEach((morph) => {
                const memento = morph.memento;
                if (this.reverse) {
                    memento.edge.meta = memento.before.meta;
                } else {
                    memento.edge.meta = memento.after.meta;
                }
            });
        } else {
            this.expanded.forEach((morph) => {
                const newRoutingPoints: Point[] = [];

                for (let i = 1; i < morph.startExpandedRoute.length - 1; i++) {
                    const startPt = morph.startExpandedRoute[i];
                    const endPt = morph.endExpandedRoute[i];
                    newRoutingPoints.push(PointUtil.linear(startPt, endPt, t));
                }

                const startSourceAnchor = morph.memento.before.sourceAnchor;
                const endSourceAnchor = morph.memento.after.sourceAnchor;
                const startTargetAnchor = morph.memento.before.targetAnchor;
                const endTargetAnchor = morph.memento.after.targetAnchor;

                const sourceAnchor = this.interpolateAnchor(startSourceAnchor, endSourceAnchor, t);
                const targetAnchor = this.interpolateAnchor(startTargetAnchor, endTargetAnchor, t);

                morph.memento.edge.meta = {
                    routingPoints: newRoutingPoints,
                    sourceAnchor: newRoutingPoints.length > 0 ? sourceAnchor : undefined,
                    targetAnchor: newRoutingPoints.length > 0 ? targetAnchor : undefined
                };
            });
        }
        return this.model;
    }

    /**
     * Interpolates between two anchors along the shortest path
     * around a rectangular node. The rectangle is treated as a
     * continuous loop with equal-length sides.
     *
     * The interpolation moves smoothly around corners and will
     * automatically choose the shortest clockwise or counter-clockwise
     * direction.
     *
     * @param start - Starting anchor
     * @param end - Ending anchor
     * @param t - Interpolation factor in the range [0, 1]
     * @returns The interpolated anchor
     */
    protected interpolateAnchor(start: EdgeAnchor, end: EdgeAnchor, t: number): EdgeAnchor {
        const startPerimeter = this.anchorToPerimeter(start);
        const endPerimeter = this.anchorToPerimeter(end);

        let delta = endPerimeter - startPerimeter;
        if (delta > 2) delta -= 4;
        if (delta < -2) delta += 4;

        const interpolated = this.wrapPerimeter(startPerimeter + delta * t);

        return this.perimeterToAnchor(interpolated);
    }

    /**
     * Converts an anchor into a perimeter position.
     * The rectangle perimeter is modeled as a loop of length 4,
     * with each side having length 1.
     *
     * Perimeter layout (clockwise):
     * - [0, 1)   : top
     * - [1, 2)   : right
     * - [2, 3)   : bottom
     * - [3, 4)   : left
     *
     * @param anchor - Anchor to convert
     * @returns Perimeter position in the range [0, 4)
     */
    private anchorToPerimeter(anchor: EdgeAnchor): number {
        switch (anchor.side) {
            case "top":
                return anchor.value;
            case "right":
                return 1 + anchor.value;
            case "bottom":
                return 2 + anchor.value;
            case "left":
                return 3 + anchor.value;
        }
    }

    /**
     * Wraps a perimeter position so it stays within [0, 4).
     *
     * @param p - Perimeter position
     * @returns Wrapped perimeter position
     */
    private wrapPerimeter(p: number): number {
        p %= 4;
        return p < 0 ? p + 4 : p;
    }

    /**
     * Converts a perimeter position back into an anchor.
     *
     * @param p - Perimeter position in the range [0, 4)
     * @returns Corresponding anchor
     */
    private perimeterToAnchor(p: number): EdgeAnchor {
        if (p < 1) {
            return { side: "top", value: p };
        }
        if (p < 2) {
            return { side: "right", value: p - 1 };
        }
        if (p < 3) {
            return { side: "bottom", value: p - 2 };
        }

        return { side: "left", value: p - 3 };
    }
}
