import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Node representing a Class in the diagram.
 */
export class ClassNode extends GNode {
    /** The name of the class */
    name?: string;
    /** Whether this class is abstract */
    isAbstract?: boolean;

    /**
     * Creates a builder for ClassNode instances.
     * 
     * @returns A new ClassNodeBuilder
     */
    static override builder(): ClassNodeBuilder {
        return new ClassNodeBuilder(ClassNode).type(MetamodelElementType.NODE_CLASS);
    }
}

/**
 * Builder for ClassNode instances.
 * Provides fluent API for constructing class nodes.
 */
export class ClassNodeBuilder<T extends ClassNode = ClassNode> extends GNodeBuilder<T> {
    /**
     * Sets the name of the class.
     * 
     * @param name The class name
     * @returns This builder for chaining
     */
    name(name: string): this {
        this.proxy.name = name;
        return this;
    }

    /**
     * Sets whether the class is abstract.
     * 
     * @param isAbstract True if the class is abstract
     * @returns This builder for chaining
     */
    isAbstract(isAbstract: boolean): this {
        this.proxy.isAbstract = isAbstract;
        return this;
    }
}
