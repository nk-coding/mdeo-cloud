import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Label for variable declarations in patterns.
 * Displays the variable name, optional type, and value expression.
 */
export class GVariableLabel extends GLabel {
    /**
     * Creates a builder for GVariableLabel instances.
     *
     * @returns A new GVariableLabelBuilder
     */
    static builder(): GVariableLabelBuilder {
        return new GVariableLabelBuilder(GVariableLabel).type(ModelTransformationElementType.LABEL_VARIABLE);
    }
}

/**
 * Builder for GVariableLabel instances.
 * Provides fluent API for constructing variable labels.
 */
export class GVariableLabelBuilder<T extends GVariableLabel = GVariableLabel> extends GLabelBuilder<T> {}
