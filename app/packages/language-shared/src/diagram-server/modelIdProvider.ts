import type { AstNode } from "langium";
import type { ModelIdRegistry } from "./modelIdRegistry.js";

/**
 * Symbol for dependency injection of ModelIdProvider.
 */
export const ModelIdProvider = Symbol("ModelIdProvider");

/**
 * Interface for providing unique IDs for AST nodes.
 * Implementations should generate IDs based on semantic information
 * from the AST node, ensuring consistency across model transformations.
 */
export interface ModelIdProvider {
    /**
     * Gets the name used for ID generation for the given AST node.
     * The name is a pure semantic name, not prefixed with the node type.
     * The registry is provided so that parent names can be looked up.
     *
     * @param node The AST node to get the name for
     * @param registry The model ID registry for looking up cached names of other nodes
     * @returns The name string or undefined if the node type is not supported
     */
    getName(node: AstNode, registry: ModelIdRegistry): string | undefined;

    /**
     * Gets called with the root node to provide additional (root) nodes to analyze and index
     *
     * @param node The root AST Node
     * @returns An array of additional AST nodes
     */
    getAdditional(node: AstNode): AstNode[];
}

/**
 * Base class for ModelIdProvider implementations.
 * Provides a default getAdditional implementation.
 */
export abstract class BaseModelIdProvider implements ModelIdProvider {
    /**
     * Gets the name used for ID generation for the given AST node.
     *
     * @param node The AST node to get the name for
     * @param registry The model ID registry for looking up cached names of other nodes
     * @returns The name string or undefined if not applicable
     */
    abstract getName(node: AstNode, registry: ModelIdRegistry): string | undefined;

    getAdditional(_node: AstNode): AstNode[] {
        return [];
    }
}
