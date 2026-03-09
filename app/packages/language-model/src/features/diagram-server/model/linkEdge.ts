import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "./elementTypes.js";

/**
 * Edge representing a link between two object instances.
 * Links connect objects optionally through specific properties.
 */
export class GLinkEdge extends GEdge {
    /**
     * The optional source property name
     */
    sourceProperty?: string;

    /**
     * The optional target property name
     */
    targetProperty?: string;

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
     * Creates a builder for GLinkEdge instances.
     *
     * @returns A new GLinkEdgeBuilder
     */
    static builder(): GLinkEdgeBuilder {
        return new GLinkEdgeBuilder(GLinkEdge).type(ModelElementType.EDGE_LINK);
    }
}

/**
 * Builder for GLinkEdge instances.
 * Provides fluent API for constructing link edges.
 */
export class GLinkEdgeBuilder<E extends GLinkEdge = GLinkEdge> extends GEdgeBuilder<E> {
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
