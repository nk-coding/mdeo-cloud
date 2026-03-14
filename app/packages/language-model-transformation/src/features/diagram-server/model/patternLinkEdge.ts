import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import type { PatternModifierKind } from "@mdeo/protocol-model-transformation";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

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
     * The modifier kind (none, create, delete, forbid, require)
     */
    modifier!: PatternModifierKind;

    /**
     * The name of the metamodel class required at the source of this link type.
     * Used by the client to validate canConnect.
     */
    sourceClass?: string;

    /**
     * The name of the metamodel class required at the target of this link type.
     * Used by the client to validate canConnect.
     */
    targetClass?: string;

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

    /**
     * Sets the source class constraint for this link type.
     *
     * @param sourceClass The metamodel class name required at the source
     * @returns This builder for chaining
     */
    sourceClass(sourceClass: string): this {
        this.proxy.sourceClass = sourceClass;
        return this;
    }

    /**
     * Sets the target class constraint for this link type.
     *
     * @param targetClass The metamodel class name required at the target
     * @returns This builder for chaining
     */
    targetClass(targetClass: string): this {
        this.proxy.targetClass = targetClass;
        return this;
    }
}
