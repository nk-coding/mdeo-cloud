import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "./elementTypes.js";

/**
 * Label for the target end of a link edge.
 * Displays the optional property specification at the target.
 */
export class GLinkTargetLabel extends GLabel {
    /**
     * Creates a builder for GLinkTargetLabel instances.
     *
     * @returns A new GLinkTargetLabelBuilder
     */
    static builder(): GLinkTargetLabelBuilder {
        return new GLinkTargetLabelBuilder(GLinkTargetLabel).type(ModelElementType.LABEL_LINK_TARGET);
    }
}

/**
 * Builder for GLinkTargetLabel instances.
 * Provides fluent API for constructing link target labels.
 */
export class GLinkTargetLabelBuilder<T extends GLinkTargetLabel = GLinkTargetLabel> extends GLabelBuilder<T> {}
