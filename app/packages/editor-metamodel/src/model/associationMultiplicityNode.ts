import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, deletableFeature } = sharedImport("@eclipse-glsp/sprotty");
const { resizeFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Client-side model for association multiplicity nodes.
 * Represents a multiplicity constraint at the start or end of an association edge.
 */
export class GAssociationMultiplicityNode extends GNode {
    static readonly DEFAULT_FEATURES = [
        boundsFeature,
        selectFeature,
        deletableFeature,
        resizeFeature,
        nodeLayoutMetadataFeature
    ];

    /**
     * Whether this multiplicity is at the start or end of the association
     */
    end!: "source" | "target";
}
