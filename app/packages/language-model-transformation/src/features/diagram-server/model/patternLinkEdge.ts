import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType, PatternModifierKind } from "./elementTypes.js";

/**
 * Edge representing a pattern link between two instances.
 * Links connect pattern instances optionally through specific properties.
 */
export class GPatternLinkEdge extends GEdge {
    /**
     * The optional source property name
     */
    sourceProperty?: string;

    /**
     * The optional target property name
     */
    targetProperty?: string;

    /**
     * The modifier kind (none, create, delete, forbid)
     */
    modifier!: PatternModifierKind;

    /**
     * Creates a builder for GPatternLinkEdge instances.
     *
     * @returns A new GPatternLinkEdgeBuilder
     */
    static builder(): GPatternLinkEdgeBuilder {
        return new GPatternLinkEdgeBuilder(GPatternLinkEdge).type(ModelTransformationElementType.EDGE_PATTERN_LINK);
    }
}

/**
 * Builder for GPatternLinkEdge instances.
 * Provides fluent API for constructing pattern link edges.
 */
export class GPatternLinkEdgeBuilder<E extends GPatternLinkEdge = GPatternLinkEdge> extends GEdgeBuilder<E> {
    /**
     * Sets the source property name.
     *
     * @param property The source property name
     * @returns This builder for chaining
     */
    sourceProperty(property: string): this {
        this.proxy.sourceProperty = property;
        return this;
    }

    /**
     * Sets the target property name.
     *
     * @param property The target property name
     * @returns This builder for chaining
     */
    targetProperty(property: string): this {
        this.proxy.targetProperty = property;
        return this;
    }

    /**
     * Sets the modifier kind.
     *
     * @param modifier The modifier kind
     * @returns This builder for chaining
     */
    modifier(modifier: PatternModifierKind): this {
        this.proxy.modifier = modifier;
        return this;
    }
}
