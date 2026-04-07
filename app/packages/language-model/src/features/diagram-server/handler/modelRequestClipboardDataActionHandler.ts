import type { AstNode } from "langium";
import { BaseRequestClipboardDataActionHandler, sharedImport, AstReflectionKey } from "@mdeo/language-shared";
import type { AstReflection } from "@mdeo/language-common";
import { ObjectInstance, Link } from "../../../grammar/modelTypes.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Model-specific action handler for {@code RequestClipboardDataAction}.
 *
 * <p>Filters selected AST nodes to the supported top-level types for the model
 * language: object instances and links.
 */
@injectable()
export class ModelRequestClipboardDataActionHandler extends BaseRequestClipboardDataActionHandler {
    /**
     * The AST reflection service for type checking.
     */
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;

    /**
     * Filters selected AST nodes to only include object instances and links.
     *
     * @param selectedAstNodes - The raw AST nodes from the selected graph elements.
     * @returns The filtered list of top-level nodes for clipboard serialization.
     */
    protected override getTopLevelAstNodes(selectedAstNodes: AstNode[]): AstNode[] {
        const topLevel: AstNode[] = [];

        for (const node of selectedAstNodes) {
            if (this.reflection.isInstance(node, ObjectInstance)) {
                topLevel.push(node);
            } else if (this.reflection.isInstance(node, Link)) {
                topLevel.push(node);
            }
        }

        return topLevel;
    }
}
