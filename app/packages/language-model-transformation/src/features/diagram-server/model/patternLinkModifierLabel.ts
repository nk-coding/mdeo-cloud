import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Server-side model for a pattern link modifier label.
 * Displayed as a child of GPatternLinkModifierNode, showing the modifier
 * keyword with guillemets (e.g. «create», «delete», «forbid», «require»).
 */
export class GPatternLinkModifierLabel extends GLabel {
    /**
     * Creates a builder for GPatternLinkModifierLabel instances.
     *
     * @returns A new GPatternLinkModifierLabelBuilder
     */
    static builder(): GPatternLinkModifierLabelBuilder {
        return new GPatternLinkModifierLabelBuilder(GPatternLinkModifierLabel).type(
            ModelTransformationElementType.LABEL_PATTERN_LINK_MODIFIER
        );
    }
}

/**
 * Builder for GPatternLinkModifierLabel instances.
 */
export class GPatternLinkModifierLabelBuilder<
    T extends GPatternLinkModifierLabel = GPatternLinkModifierLabel
> extends GLabelBuilder<T> {}
