import { GNode, GNodeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "@mdeo/protocol-metamodel";

/**
 * Node representing an Enum in the diagram.
 */
export class GEnumNode extends GNode {
    /**
     * The name of the enum
     */
    name!: string;

    /**
     * Creates a builder for GEnumNode instances.
     *
     * @returns A new GEnumNodeBuilder
     */
    static builder(): GEnumNodeBuilder {
        return new GEnumNodeBuilder(GEnumNode).type(MetamodelElementType.NODE_ENUM);
    }
}

/**
 * Builder for GEnumNode instances.
 * Provides fluent API for constructing enum nodes.
 */
export class GEnumNodeBuilder<T extends GEnumNode = GEnumNode> extends GNodeBuilder<T> {
    /**
     * Sets the name of the enum.
     *
     * @param name The enum name
     * @returns This builder for chaining
     */
    name(name: string): this {
        this.proxy.name = name;
        return this;
    }
}
