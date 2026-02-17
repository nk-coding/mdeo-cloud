import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Label for where clause conditions in patterns.
 * Displays the expression constraint.
 */
export class GWhereClauseLabel extends GLabel {
    /**
     * Creates a builder for GWhereClauseLabel instances.
     *
     * @returns A new GWhereClauseLabelBuilder
     */
    static builder(): GWhereClauseLabelBuilder {
        return new GWhereClauseLabelBuilder(GWhereClauseLabel).type(ModelTransformationElementType.LABEL_WHERE_CLAUSE);
    }
}

/**
 * Builder for GWhereClauseLabel instances.
 * Provides fluent API for constructing where clause labels.
 */
export class GWhereClauseLabelBuilder<T extends GWhereClauseLabel = GWhereClauseLabel> extends GLabelBuilder<T> {}
