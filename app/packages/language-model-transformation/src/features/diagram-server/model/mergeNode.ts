import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Merge node where control flow branches join back together.
 * Used after if/else constructs to merge the paths.
 */
export class GMergeNode extends GNode {
    /**
     * Creates a builder for GMergeNode instances.
     *
     * @returns A new GMergeNodeBuilder
     */
    static builder(): GMergeNodeBuilder {
        return new GMergeNodeBuilder(GMergeNode).type(ModelTransformationElementType.NODE_MERGE);
    }
}

/**
 * Builder for GMergeNode instances.
 * Provides fluent API for constructing merge nodes.
 */
export class GMergeNodeBuilder<T extends GMergeNode = GMergeNode> extends GNodeBuilder<T> {}
