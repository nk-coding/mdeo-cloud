import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, moveFeature, connectableFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for call nodes: a call of another transformation, shown as its text.
 */
export class GCallNode extends GNode {
    static readonly DEFAULT_FEATURES = [
        boundsFeature,
        selectFeature,
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
     * The call as written, e.g. `call moveComponent(maxCandidates = 2) -> (moved: m)`.
     */
    text!: string;
}
