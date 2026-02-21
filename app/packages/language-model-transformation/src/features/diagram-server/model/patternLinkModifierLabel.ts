import { GModelElement, GModelElementBuilder } from "@mdeo/language-shared";
import type { PatternModifierKind } from "./elementTypes.js";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Server-side model for a pattern link modifier label.
 * Displayed in the middle of a pattern link edge when a modifier
 * (create/delete/forbid) is present on the link.
 */
export class GPatternLinkModifierLabel extends GModelElement {
    /**
     * The modifier kind driving which text/colour to show
     */
    modifier!: PatternModifierKind;

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
> extends GModelElementBuilder<T> {
    /**
     * Sets the modifier kind.
     *
     * @param modifier The pattern modifier kind
     * @returns This builder for chaining
     */
    modifier(modifier: PatternModifierKind): this {
        this.proxy.modifier = modifier;
        return this;
    }
}
