import type { AstNode } from "langium";
import { AstUtils } from "langium";
import type { ModelIdProvider } from "./modelIdProvider.js";

/**
 * Registry for managing unique IDs for all AST nodes in a model.
 * This class generates IDs for all nodes at initialization time and handles
 * uniqueness constraints by automatically appending counters when needed.
 */
export class ModelIdRegistry {
    private readonly idMap = new Map<AstNode, string>();
    private readonly usedIds = new Set<string>();

    /**
     * Creates a new ModelIdRegistry by traversing the model and generating IDs.
     *
     * @param rootNode - The root AST node of the model
     * @param idProvider - The ID provider to use for generating base IDs
     */
    constructor(
        rootNode: AstNode,
        private readonly idProvider: ModelIdProvider
    ) {
        this.generateIds(rootNode);
    }

    /**
     * Retrieves the ID for the given AST node.
     *
     * @param node - The AST node to get the ID for
     * @returns The ID or undefined if not found
     */
    getId(node: AstNode): string | undefined {
        return this.idMap.get(node);
    }

    /**
     * Checks if an ID has been assigned to the given node.
     *
     * @param node - The AST node to check
     * @returns True if the node has an ID
     */
    hasId(node: AstNode): boolean {
        return this.idMap.has(node);
    }

    /**
     * Generates IDs for all nodes in the model tree.
     *
     * @param rootNode - The root node to start traversal from
     */
    private generateIds(rootNode: AstNode): void {
        const stream = AstUtils.streamAllContents(rootNode);

        for (const node of stream) {
            const baseId = this.idProvider.getId(node);
            if (baseId !== undefined) {
                const uniqueId = this.ensureUnique(baseId);
                this.idMap.set(node, uniqueId);
                this.usedIds.add(uniqueId);
            }
        }
    }

    /**
     * Ensures the given ID is unique by appending a counter if necessary.
     *
     * @param baseId - The base ID to make unique
     * @returns A unique ID
     */
    private ensureUnique(baseId: string): string {
        if (!this.usedIds.has(baseId)) {
            return baseId;
        }

        let counter = 1;
        let candidateId = `${baseId}_${counter}`;

        while (this.usedIds.has(candidateId)) {
            counter++;
            candidateId = `${baseId}_${counter}`;
        }

        return candidateId;
    }
}
