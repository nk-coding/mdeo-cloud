import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { ModelElementType } from "./elementTypes.js";

/**
 * Node representing an ObjectInstance in the diagram.
 * Contains the object's name and its class type reference.
 */
export class GObjectNode extends GNode {
    /**
     * The name of the object instance
     */
    name!: string;

    /**
     * The type name (class name) of the object instance
     */
    typeName!: string;

    /**
     * The class hierarchy of this instance (class name and all superclass names).
     * Used by the client to validate canConnect against association source/target classes.
     */
    classHierarchy: string[] = [];

    /**
     * Creates a builder for GObjectNode instances.
     *
     * @returns A new GObjectNodeBuilder
     */
    static builder(): GObjectNodeBuilder {
        return new GObjectNodeBuilder(GObjectNode).type(ModelElementType.NODE_OBJECT);
    }
}

/**
 * Builder for GObjectNode instances.
 * Provides fluent API for constructing object nodes.
 */
export class GObjectNodeBuilder<T extends GObjectNode = GObjectNode> extends GNodeBuilder<T> {
    /**
     * Sets the name of the object instance.
     *
     * @param name The object name
     * @returns This builder for chaining
     */
    name(name: string): this {
        this.proxy.name = name;
        return this;
    }

    /**
     * Sets the type name (class name) of the object instance.
     *
     * @param typeName The type/class name
     * @returns This builder for chaining
     */
    typeName(typeName: string): this {
        this.proxy.typeName = typeName;
        return this;
    }

    /**
     * Sets the class hierarchy of the object instance.
     *
     * @param hierarchy Array of class names from the instance class up through its superclasses
     * @returns This builder for chaining
     */
    classHierarchy(hierarchy: string[]): this {
        this.proxy.classHierarchy = hierarchy;
        return this;
    }
}
