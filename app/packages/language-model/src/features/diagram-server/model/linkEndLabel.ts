import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "@mdeo/protocol-model";

/**
 * Label for link end information (source or target).
 * Displays the optional property specification at the link end.
 */
export class GLinkEndLabel extends GLabel {
    /**
     * Creates a builder for GLinkEndLabel instances.
     *
     * @returns A new GLinkEndLabelBuilder
     */
    static builder(): GLinkEndLabelBuilder {
        return new GLinkEndLabelBuilder(GLinkEndLabel).type(ModelElementType.LABEL_LINK_END);
    }
}

/**
 * Builder for GLinkEndLabel instances.
 * Provides fluent API for constructing link end labels.
 */
export class GLinkEndLabelBuilder<T extends GLinkEndLabel = GLinkEndLabel> extends GLabelBuilder<T> {}
