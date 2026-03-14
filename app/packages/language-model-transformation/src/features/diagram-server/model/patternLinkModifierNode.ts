import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import type { PatternModifierKind } from "@mdeo/protocol-model-transformation";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Server-side model for a pattern link modifier node.
 * Wraps the modifier label to provide proper bounds handling for the
 * «create»/«delete»/«forbid»/«require» badge in the middle of a pattern link edge.
 */
export class GPatternLinkModifierNode extends GNode {
    /**
     * The modifier kind driving which colour to apply
     */
    modifier!: PatternModifierKind;

    /**
     * Creates a builder for GPatternLinkModifierNode instances.
     *
     * @returns A new GPatternLinkModifierNodeBuilder
     */
    static builder(): GPatternLinkModifierNodeBuilder {
        return new GPatternLinkModifierNodeBuilder(GPatternLinkModifierNode).type(
            ModelTransformationElementType.NODE_PATTERN_LINK_MODIFIER
        );
    }
}

/**
 * Builder for GPatternLinkModifierNode instances.
 */
export class GPatternLinkModifierNodeBuilder<
    T extends GPatternLinkModifierNode = GPatternLinkModifierNode
> extends GNodeBuilder<T> {
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
