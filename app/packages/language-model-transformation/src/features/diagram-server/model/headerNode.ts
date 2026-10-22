import { GLabel, GLabelBuilder, GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * The header box of a transformation, drawn above its start node: the transformation's name,
 * its path imports and its `in` / `out` declarations, one editable row per header line.
 */
export class GHeaderNode extends GNode {
    /**
     * Creates a builder for GHeaderNode instances.
     *
     * @returns A new GHeaderNodeBuilder
     */
    static builder(): GHeaderNodeBuilder {
        return new GHeaderNodeBuilder(GHeaderNode).type(ModelTransformationElementType.NODE_HEADER);
    }
}

/**
 * Builder for GHeaderNode instances.
 */
export class GHeaderNodeBuilder<T extends GHeaderNode = GHeaderNode> extends GNodeBuilder<T> {}

/**
 * The title of the header box: the name of the transformation's file.
 */
export class GHeaderTitleLabel extends GLabel {
    /**
     * Creates a builder for GHeaderTitleLabel instances.
     *
     * @returns A new GHeaderTitleLabelBuilder
     */
    static builder(): GHeaderTitleLabelBuilder {
        return new GHeaderTitleLabelBuilder(GHeaderTitleLabel)
            .type(ModelTransformationElementType.LABEL_HEADER_TITLE)
            .readonly(true);
    }
}

/**
 * Builder for GHeaderTitleLabel instances.
 */
export class GHeaderTitleLabelBuilder<T extends GHeaderTitleLabel = GHeaderTitleLabel> extends GLabelBuilder<T> {}

/**
 * A row of the header box: one header line as written (`import "./x.mt" as x`,
 * `in maxLoad: int = 3`). Editing it replaces the line; deleting it removes the line.
 */
export class GHeaderRowLabel extends GLabel {
    /**
     * Creates a builder for a row holding a path import.
     *
     * @returns A new GHeaderRowLabelBuilder
     */
    static importBuilder(): GHeaderRowLabelBuilder {
        return new GHeaderRowLabelBuilder(GHeaderRowLabel).type(ModelTransformationElementType.LABEL_HEADER_IMPORT);
    }

    /**
     * Creates a builder for a row holding an `in` / `out` declaration.
     *
     * @returns A new GHeaderRowLabelBuilder
     */
    static declarationBuilder(): GHeaderRowLabelBuilder {
        return new GHeaderRowLabelBuilder(GHeaderRowLabel).type(
            ModelTransformationElementType.LABEL_HEADER_DECLARATION
        );
    }
}

/**
 * Builder for GHeaderRowLabel instances.
 */
export class GHeaderRowLabelBuilder<T extends GHeaderRowLabel = GHeaderRowLabel> extends GLabelBuilder<T> {}
