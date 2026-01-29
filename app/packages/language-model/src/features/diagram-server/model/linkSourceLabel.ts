import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "./elementTypes.js";

/**
 * Label for the source end of a link edge.
 * Displays the optional property specification at the source.
 */
export class GLinkSourceLabel extends GLabel {
    /**
     * Creates a builder for GLinkSourceLabel instances.
     *
     * @returns A new GLinkSourceLabelBuilder
     */
    static builder(): GLinkSourceLabelBuilder {
        return new GLinkSourceLabelBuilder(GLinkSourceLabel).type(ModelElementType.LABEL_LINK_SOURCE);
    }
}

/**
 * Builder for GLinkSourceLabel instances.
 * Provides fluent API for constructing link source labels.
 */
export class GLinkSourceLabelBuilder<T extends GLinkSourceLabel = GLinkSourceLabel> extends GLabelBuilder<T> {}
