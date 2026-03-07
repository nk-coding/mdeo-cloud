import type { GNode, ModelState, GModelIndex } from "@mdeo/language-shared";
import { BaseCreateEdgeOperationHandler, sharedImport } from "@mdeo/language-shared";
import type { AstNode, CstNode } from "langium";
import { Class, type ClassType } from "../../../grammar/metamodelTypes.js";
import type { WorkspaceEdit } from "vscode-languageserver-types";

const { injectable, inject } = sharedImport("inversify");
const { ModelState: ModelStateKey, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Shared base handler for metamodel create-edge operations.
 * Provides the common model-state injections and utility methods used by
 * both association and inheritance edge handlers.
 */
@injectable()
export abstract class MetamodelBaseCreateEdgeOperationHandler extends BaseCreateEdgeOperationHandler {
    @inject(ModelStateKey)
    protected readonly localModelState!: ModelState;

    @inject(GModelIndexKey)
    protected readonly localIndex!: GModelIndex;

    /**
     * Resolves a GModel node to its ClassType AST node.
     * Returns undefined if the node is not a Class (e.g., it is an Enum).
     *
     * @param node The GNode to resolve
     * @returns The ClassType AST node, or undefined if the node is not a Class
     */
    protected resolveClass(node: GNode): ClassType | undefined {
        const astNode = this.localIndex.getAstNode(node) as AstNode | undefined;
        if (!astNode || !this.localModelState.languageServices.shared.AstReflection.isInstance(astNode, Class)) {
            return undefined;
        }
        return astNode as ClassType;
    }

    /**
     * Creates a workspace edit that inserts text immediately after a CST node.
     *
     * @param node The CST node after which to insert text
     * @param text The text to insert
     * @returns The workspace edit performing the insertion
     */
    protected insertAfterCstNode(node: CstNode, text: string): WorkspaceEdit {
        const document = this.modelState.sourceModel?.$document;
        if (!document) {
            throw new Error("Source model document not available.");
        }
        const pos = document.textDocument.positionAt(node.end);
        return {
            changes: {
                [this.modelState.sourceUri!]: [{ range: { start: pos, end: pos }, newText: text }]
            }
        };
    }
}
