import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, deletableFeature, moveFeature, connectableFeature } =
    sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for diamond nodes.
 * Represents a decision point (if/while branching) in the control flow.
 */
export class GDiamondNode extends GNode {
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
