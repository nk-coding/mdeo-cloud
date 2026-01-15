import type { AstNode } from "langium";

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
     * Generates a unique ID for the given AST node.
     * The ID should be based on semantic information and be deterministic.
     *
     * @param node The AST node to generate an ID for
     * @returns The generated ID or undefined if the node type is not supported
     */
    getId(node: AstNode): string | undefined;

    /**
     * Gets additional AST nodes that should be assigned the same ID as the given node.
     * This should be used when multiple AST nodes semantically represent the same entity.
     *
     * @param node The AST node to get additional nodes for
     * @returns An array of additional AST nodes
     */
    getAdditional(node: AstNode): AstNode[];
}

/**
 * Base class for ModelIdProvider implementations.
 * Provides a default implementation that constructs IDs
 * using the node type and a name extracted from the node.
 */
export abstract class BaseModelIdProvider implements ModelIdProvider {
    getId(node: AstNode): string | undefined {
        const name = this.getName(node);
        if (name !== undefined) {
            return `${node.$type}_${name}`;
        }
        return undefined;
    }

    /**
     * Gets the name used for ID generation for the given AST node.
     *
     * @param node The AST node to get the name for
     * @returns The name string or undefined if not applicable
     */
    abstract getName(node: AstNode): string | undefined;

    getAdditional(_node: AstNode): AstNode[] {
        return [];
    }
}
