import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, deletableFeature } = sharedImport("@eclipse-glsp/sprotty");
const { resizeFeature } = sharedImport("@eclipse-glsp/client");

/**
 * Client-side model for association property nodes.
 * Represents a property name at the start or end of an association edge.
 */
export class GAssociationPropertyNode extends GNode {
    static readonly DEFAULT_FEATURES = [
        boundsFeature,
        selectFeature,
        deletableFeature,
        resizeFeature,
        nodeLayoutMetadataFeature
    ];

    /**
     * Whether this property is at the start or end of the association
     */
    end!: "source" | "target";
}
