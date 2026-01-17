import type { Locateable, Selectable } from "@eclipse-glsp/protocol";
import { sharedImport } from "../sharedImport.js";
import type { EdgeLayoutMetadata } from "@mdeo/editor-protocol";

const { GChildElement, selectFeature, moveFeature, Point } = sharedImport("@eclipse-glsp/sprotty");

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
    readonly position = Point.ORIGIN;
}
