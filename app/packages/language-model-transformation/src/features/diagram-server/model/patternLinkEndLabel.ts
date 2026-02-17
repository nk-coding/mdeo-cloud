import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Label for pattern link end information (source or target).
 * Displays the optional property specification at the link end.
 */
export class GPatternLinkEndLabel extends GLabel {
    /**
     * Creates a builder for GPatternLinkEndLabel instances.
     *
     * @returns A new GPatternLinkEndLabelBuilder
     */
    static builder(): GPatternLinkEndLabelBuilder {
        return new GPatternLinkEndLabelBuilder(GPatternLinkEndLabel).type(
            ModelTransformationElementType.LABEL_PATTERN_LINK_END
        );
    }
}

/**
 * Builder for GPatternLinkEndLabel instances.
 * Provides fluent API for constructing pattern link end labels.
 */
export class GPatternLinkEndLabelBuilder<
    T extends GPatternLinkEndLabel = GPatternLinkEndLabel
> extends GLabelBuilder<T> {}
