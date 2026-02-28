import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import type { PatternModifierKind } from "./elementTypes.js";
import { ModelTransformationElementType } from "./elementTypes.js";

/**
 * Node representing a pattern object instance.
 * Contains the instance's name, optional type, and optional modifier.
 */
export class GPatternInstanceNode extends GNode {
    /**
     * The name of the pattern instance
     */
    name!: string;

    /**
     * The optional type name (class name) of the pattern instance
     */
    typeName?: string;

    /**
     * The modifier kind (none, create, delete, forbid, require)
     */
    modifier!: PatternModifierKind;

    /**
     * Creates a builder for GPatternInstanceNode instances.
     *
     * @returns A new GPatternInstanceNodeBuilder
     */
    static builder(): GPatternInstanceNodeBuilder {
        return new GPatternInstanceNodeBuilder(GPatternInstanceNode).type(
            ModelTransformationElementType.NODE_PATTERN_INSTANCE
        );
    }
}

/**
 * Builder for GPatternInstanceNode instances.
 * Provides fluent API for constructing pattern instance nodes.
 */
export class GPatternInstanceNodeBuilder<
    T extends GPatternInstanceNode = GPatternInstanceNode
> extends GNodeBuilder<T> {
    /**
     * Sets the name of the pattern instance.
     *
     * @param name The instance name
     * @returns This builder for chaining
     */
    name(name: string): this {
        this.proxy.name = name;
        return this;
    }

    /**
     * Sets the type name (class name) of the pattern instance.
     *
     * @param typeName The type/class name
     * @returns This builder for chaining
     */
    typeName(typeName: string): this {
        this.proxy.typeName = typeName;
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
