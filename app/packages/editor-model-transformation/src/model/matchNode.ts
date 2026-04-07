import { GEdge, GIssueMarker, GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";
import type { GModelElement, Point, Bounds } from "@eclipse-glsp/sprotty";
import { GMatchNodeView } from "../views/matchNodeView.js";
import { GMatchNodeCompartments } from "./matchNodeCompartments.js";
import { GControlFlowEdge } from "./controlFlowEdge.js";

const {
    connectableFeature,
    deletableFeature,
    selectFeature,
    boundsFeature,
    moveFeature,
    fadeFeature,
    layoutContainerFeature,
    Bounds: BoundsUtil,
    isBounds,
    isBoundsAware
} = sharedImport("@eclipse-glsp/sprotty");
const { containerFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Render information derived from a match node, used by both the view and the
 * coordinate-conversion helpers on the model.
 */
export interface MatchNodeRenderInfo {
    /**
     * All non-container children (pattern instances + edge children combined)
     */
    innerChildren: GModelElement[];
    /**
     * The optional container node (variables / where-clause compartments)
     */
    containerNode: GMatchNodeCompartments | undefined;
    /**
     * Bounding box of all inner children in their local (pre-translate) coordinate space
     */
    innerChildrenBounds: Bounds;
    /**
     * The SVG `translate(x, y)` offset applied to the inner children group inside
     * the view.  Does **not** include this node's own position.
     */
    innerChildrenTranslation: Point;
}

/**
 * Client-side model for a match node in the transformation diagram.
 * Match nodes contain pattern elements (instances, links) as children,
 * and may also contain constraint compartments (variables, where clauses).
 */
export class GMatchNode extends GNode {
    /**
     * Default features enabled for match nodes
     */
    static readonly DEFAULT_FEATURES = [
        connectableFeature,
        deletableFeature,
        selectFeature,
        boundsFeature,
        moveFeature,
        fadeFeature,
        nodeLayoutMetadataFeature,
        layoutContainerFeature,
        containerFeature
    ];

    /**
     * Whether this is a "for match" (multiple matches iteration)
     */
    multiple!: boolean;

    /**
     * Computes the bounding box of all given inner children (pattern instances and
     * edges), including routing points and bounds-aware sub-children of edges.
     */
    private computeInnerChildrenBounds(innerChildren: GModelElement[]): Bounds {
        if (innerChildren.length === 0) {
            return { x: 0, y: 0, width: GMatchNodeView.MIN_CONTENT_SIZE, height: GMatchNodeView.MIN_CONTENT_SIZE };
        }

        let combined: Bounds | undefined;
        const expandWithBounds = (bounds: Bounds) => {
            combined = combined === undefined ? bounds : BoundsUtil.combine(combined, bounds);
        };

        for (const child of innerChildren) {
            if (isBoundsAware(child)) {
                expandWithBounds(child.bounds);
            }

            if (child instanceof GEdge) {
                for (const routingPoint of child.meta.routingPoints) {
                    expandWithBounds({ x: routingPoint.x, y: routingPoint.y, width: 0, height: 0 });
                }
            }
        }

        return (
            combined ?? { x: 0, y: 0, width: GMatchNodeView.MIN_CONTENT_SIZE, height: GMatchNodeView.MIN_CONTENT_SIZE }
        );
    }

    /**
     * Returns render information for this match node, shared between the view
     * (for rendering) and the coordinate helpers (for parentToLocal / localToParent).
     */
    getRenderInfo(): MatchNodeRenderInfo {
        const innerChildren: GModelElement[] = [];
        let containerNode: GMatchNodeCompartments | undefined;

        for (const child of this.children) {
            if (child instanceof GMatchNodeCompartments) {
                containerNode = child;
            } else if (!(child instanceof GIssueMarker)) {
                innerChildren.push(child);
            }
        }

        const innerChildrenBounds = this.computeInnerChildrenBounds(innerChildren);
        const innerChildrenTranslation: Point = {
            x: GMatchNodeView.INNER_PADDING - innerChildrenBounds.x,
            y: GMatchNodeView.INNER_PADDING - innerChildrenBounds.y
        };

        return { innerChildren, containerNode, innerChildrenBounds, innerChildrenTranslation };
    }

    /**
     * Returns the SVG translate offset applied to child nodes in the view,
     * shifted by this node's own position to yield a parent-space transform.
     */
    private get childOffset(): Point {
        const { innerChildrenTranslation: t } = this.getRenderInfo();
        const position = this.position;
        return {
            x: t.x + position.x,
            y: t.y + position.y
        };
    }

    /**
     * Converts a point/bounds from this element's parent coordinate system to the
     * local coordinate system used by children (subtracts the SVG translate offset).
     */
    override parentToLocal(point: Point | Bounds): Bounds {
        const { x: tx, y: ty } = this.childOffset;
        if (isBounds(point)) {
            return { x: point.x - tx, y: point.y - ty, width: point.width, height: point.height };
        }
        return { x: point.x - tx, y: point.y - ty, width: -1, height: -1 };
    }

    /**
     * Converts a point/bounds from the local coordinate system of children back to
     * this element's parent coordinate system (adds the SVG translate offset).
     */
    override localToParent(point: Point | Bounds): Bounds {
        const { x: tx, y: ty } = this.childOffset;
        if (isBounds(point)) {
            return { x: point.x + tx, y: point.y + ty, width: point.width, height: point.height };
        }
        return { x: point.x + tx, y: point.y + ty, width: -1, height: -1 };
    }

    override canConnect(edge: GEdge): boolean {
        return edge instanceof GControlFlowEdge;
    }
}

/**
 * Type guard to check if an element is a match node.
 *
 * @param element The model element to check
 * @returns True if the element is a GMatchNode
 */
export function isMatchNode(element: GModelElement): element is GMatchNode {
    return element instanceof GMatchNode;
}
