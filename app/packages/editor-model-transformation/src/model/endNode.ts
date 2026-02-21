import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";
import type { EndNodeKind } from "./elementTypes.js";

const { boundsFeature, selectFeature, deletableFeature, moveFeature, connectableFeature } =
    sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for end nodes.
 * Represents the end point of a transformation control flow (stop or kill).
 */
export class GEndNode extends GNode {
    static readonly DEFAULT_FEATURES = [
        boundsFeature,
        selectFeature,
        deletableFeature,
        moveFeature,
        connectableFeature,
        nodeLayoutMetadataFeature
    ];

    constructor() {
        super();
        this.vAlign = "center";
        this.hAlign = "center";
    }

    /**
     * The kind of end node (stop or kill)
     */
    kind!: EndNodeKind;
}
