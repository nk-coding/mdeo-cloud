import { GLabel, GLabelBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Server-side model for a pattern instance modifier label.
 * Displays the modifier keyword with guillemets (e.g. «create», «delete»,
 * «forbid», «require») inside the modifier title compartment of a pattern instance node.
 */
export class GPatternModifierLabel extends GLabel {
    /**
     * Creates a builder for GPatternModifierLabel instances.
     *
     * @returns A new GPatternModifierLabelBuilder
     */
    static builder(): GPatternModifierLabelBuilder {
        return new GPatternModifierLabelBuilder(GPatternModifierLabel).type(
            ModelTransformationElementType.LABEL_PATTERN_MODIFIER
        );
    }
}

/**
 * Builder for GPatternModifierLabel instances.
 */
export class GPatternModifierLabelBuilder<
    T extends GPatternModifierLabel = GPatternModifierLabel
> extends GLabelBuilder<T> {}
