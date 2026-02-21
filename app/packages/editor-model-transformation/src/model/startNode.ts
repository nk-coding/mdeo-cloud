import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, deletableFeature, moveFeature, connectableFeature } =
    sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for start nodes.
 * Represents the start point of a transformation control flow.
 */
export class GStartNode extends GNode {
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
}
