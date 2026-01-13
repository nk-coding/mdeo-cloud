import type { AstNode } from "langium";
import type { ModelIdProvider } from "./modelIdProvider.js";
import { sharedImport } from "../sharedImport.js";

const { AstUtils } = sharedImport("langium");

/**
 * Registry for managing unique IDs for all AST nodes in a model.
 * This class generates IDs for all nodes at initialization time and handles
 * uniqueness constraints by automatically appending counters when needed.
 */
export class ModelIdRegistry {
    /**
     * Mapping from AST nodes to their assigned unique IDs.
     */
    private readonly idMap = new Map<AstNode, string>();
    /**
     * Set of already used IDs to ensure uniqueness.
     */
    private readonly usedIds = new Set<string>();
    /**
     * Counter for generating IDs for unresolved nodes.
     */
    private unresolvedCounter = 0;

    /**
     * Creates a new ModelIdRegistry by traversing the model and generating IDs.
     *
     * @param rootNode The root AST node of the model
     * @param idProvider The ID provider to use for generating base IDs
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
     * @param node The AST node to get the ID for
     * @returns The ID
     * @throws Error if the node does not have an assigned ID
     */
    getId(node: AstNode): string {
        const id = this.idMap.get(node);
        if (id == undefined) {
            throw new Error(`No ID assigned for node: ${node}`);
        }
        return id;
    }

    /**
     * Retrieves the ID for the given AST node.
     * If not found, constructs an "unresolved" ID based on the node type and a unique counter.
     * Use with caution, as this may lead to non-deterministic IDs.
     * This is mainly useful for ids for external entities where neigher edit nor metadata is available or required.
     *
     * @param node The AST node to get the ID for
     * @returns The ID, or an "unresolved" ID if not assigned
     */
    getIdOrUnresolved(node: AstNode): string {
        const id = this.idMap.get(node);
        if (id == undefined) {
            const unresolvedId = `unresolved_${node.$type}_${this.unresolvedCounter++}`;
            this.usedIds.add(unresolvedId);
            this.idMap.set(node, unresolvedId);
            return unresolvedId;
        }
        return id;
    }

    /**
     * Checks if the given AST node has an assigned ID.
     *
     * @param node The AST node to check
     * @returns True if the node has an ID, false otherwise
     */
    hasId(node: AstNode): boolean {
        return this.idMap.has(node);
    }

    /**
     * Generates IDs for all nodes in the model tree.
     *
     * @param rootNode The root node to start traversal from
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
     * @param baseId The base ID to make unique
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
