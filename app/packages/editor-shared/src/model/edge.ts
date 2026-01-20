import type { Locateable, Selectable } from "@eclipse-glsp/protocol";
import { sharedImport } from "../sharedImport.js";
import type { EdgeLayoutMetadata, EdgeAnchor } from "@mdeo/editor-protocol";
import type { Point } from "@eclipse-glsp/protocol";

const { GChildElement, selectFeature, moveFeature, Point: PointOrigin } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Type of reconnect end - source or target.
 */
export type ReconnectEnd = "source" | "target";

/**
 * Data for an edge in reconnect mode.
 * Either has a free position (when not over a valid target) or an anchor (when snapped to a target).
 */
export interface EdgeReconnectData {
    /**
     * Which end is being reconnected.
     */
    end: ReconnectEnd;
    /**
     * The current free position of the reconnecting end (used when not snapped to a target).
     */
    position?: Point;
    /**
     * The anchor when snapped to a target node.
     */
    anchor?: EdgeAnchor;
    /**
     * The ID of the target element if hovering over a valid target.
     */
    targetId?: string;
}

/**
 * Base client-side model for edge elements.
 * Extends the GLSP edge implementation with metadata and routing support.
 */
export class GEdge extends GChildElement implements Selectable, Locateable {
    static readonly DEFAULT_FEATURES = [selectFeature, moveFeature];

    /**
     * The metadata for this edge, including routing information.
     */
    meta!: EdgeLayoutMetadata;

    /**
     * The ID of the source element this edge connects from
     */
    sourceId!: string;

    /**
     * The ID of the target element this edge connects to
     */
    targetId!: string;

    /**
     * Whether this edge is currently selected
     */
    selected: boolean = false;

    /**
     * Required to not drag the diagram when interacting with the edge
     */
    readonly position = PointOrigin.ORIGIN;

    /**
     * Data for reconnect mode, if the edge is currently being reconnected.
     */
    reconnectData?: EdgeReconnectData;
}
