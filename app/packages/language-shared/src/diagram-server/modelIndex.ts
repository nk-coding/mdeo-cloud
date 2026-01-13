import type { AstNode } from "langium";
import { sharedImport } from "../sharedImport.js";
import type { ModelIdRegistry } from "./modelIdRegistry.js";
import type { GModelElement } from "@eclipse-glsp/server";

const { injectable } = sharedImport("inversify");
const { GModelIndex: BaseGModelIndex } = sharedImport("@eclipse-glsp/server");
const { AstUtils } = sharedImport("langium");

/**
 * Index for efficient lookup of graph model elements.
 * Extends the base GLSP GModelIndex implementation.
 */
@injectable()
export class GModelIndex extends BaseGModelIndex {
    /**
     * Mapping from model element IDs to their corresponding AST nodes.
     */
    protected idToAstNode = new Map<string, AstNode>();

    /**
     * Indexes the source model root AST nodes.
     *
     * @param root the source model root AST node
     * @param idRegistry the model ID registry to get IDs
     */
    indexSourceModelRoot(root: AstNode, idRegistry: ModelIdRegistry): void {
        this.idToAstNode.clear();
        for (const node of AstUtils.streamAst(root)) {
            if (idRegistry.hasId(node)) {
                this.idToAstNode.set(idRegistry.getId(node), node);
            }
        }
    }

    /**
     * Gets the AST node corresponding to the given graph model element.
     *
     * @param node the graph model element
     * @returns the corresponding AST node, or undefined if not found
     */
    getAstNode(node: GModelElement): AstNode | undefined {
        const normalizedId = this.normalizeId(node.id);
        return this.idToAstNode.get(normalizedId);
    }

    /**
     * Normalizes the given ID by removing any fragment identifiers.
     *
     * @param id the ID to normalize
     * @returns the normalized ID
     */
    private normalizeId(id: string): string {
        const hashIndex = id.indexOf("#");
        if (hashIndex >= 0) {
            return id.substring(0, hashIndex);
        }
        return id;
    }
}
