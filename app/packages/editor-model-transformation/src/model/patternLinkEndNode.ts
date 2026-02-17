import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, deletableFeature } = sharedImport("@eclipse-glsp/sprotty");
const { resizeFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Client-side model for pattern link end nodes (source or target).
 * Wraps the end label to provide proper bounds handling.
 */
export class GPatternLinkEndNode extends GNode {
    static readonly DEFAULT_FEATURES = [
        boundsFeature,
        selectFeature,
        deletableFeature,
        resizeFeature,
        nodeLayoutMetadataFeature
    ];

    /**
     * Whether this is at the source or target end of the link
     */
    end!: "source" | "target";
}
