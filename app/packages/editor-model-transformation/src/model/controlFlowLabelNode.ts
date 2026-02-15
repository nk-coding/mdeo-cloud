import { GNode, nodeLayoutMetadataFeature, sharedImport } from "@mdeo/editor-shared";

const { boundsFeature, selectFeature, deletableFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for control flow label nodes.
 * Wraps the control flow label to provide proper bounds handling.
 */
export class GControlFlowLabelNode extends GNode {
    static readonly DEFAULT_FEATURES = [boundsFeature, selectFeature, deletableFeature, nodeLayoutMetadataFeature];

    /**
     * Whether this label is at the source or target end of the edge
     */
    end!: "source" | "target";
}
