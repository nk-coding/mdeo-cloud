import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, deletableFeature, moveFeature, connectableFeature } =
    sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for merge nodes.
 * Represents where control flow branches join back together.
 */
export class GMergeNode extends GNode {
    static readonly DEFAULT_FEATURES = [
        boundsFeature,
        selectFeature,
        deletableFeature,
        moveFeature,
        connectableFeature,
        nodeLayoutMetadataFeature
    ];
}
