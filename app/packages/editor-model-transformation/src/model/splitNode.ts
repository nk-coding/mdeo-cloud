import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, deletableFeature, moveFeature, connectableFeature } =
    sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for split nodes.
 * Represents a decision/branching point (if/while) in the control flow.
 */
export class GSplitNode extends GNode {
    static readonly DEFAULT_FEATURES = [
        boundsFeature,
        selectFeature,
        deletableFeature,
        moveFeature,
        connectableFeature,
        nodeLayoutMetadataFeature
    ];

    /**
     * The expression text displayed in the diamond
     */
    expression!: string;
}
