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
     * @param node - The AST node to generate an ID for
     * @returns The generated ID or undefined if the node type is not supported
     */
    getId(node: AstNode): string | undefined;
}
