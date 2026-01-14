import type {
    MaybePromise,
    Command,
    ApplyLabelEditOperation as ApplyLabelEditOperationType
} from "@eclipse-glsp/server";
import { ID } from "@mdeo/language-common";
import type { AstNode, CstNode } from "langium";
import type { TextEdit as TextEditType, WorkspaceEdit } from "vscode-languageserver-types";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";

const { injectable } = sharedImport("inversify");
const { ApplyLabelEditOperation } = sharedImport("@eclipse-glsp/protocol");
const { TextEdit } = sharedImport("vscode-languageserver-types");

/**
 * Handler for applying label edit operations from the client.
 */
@injectable()
export abstract class BaseApplyLabelEditOperationHandler extends BaseOperationHandler {
    override readonly operationType = ApplyLabelEditOperation.KIND;

    override createCommand(operation: ApplyLabelEditOperationType): MaybePromise<Command | undefined> {
        const label = this.modelState.index.find(operation.labelId);
        if (label == undefined) {
            throw new Error(`Label with ID ${operation.labelId} not found in model.`);
        }
        const sourceElement = this.index.getAstNode(label);

        if (sourceElement == undefined) {
            throw new Error(`AST node for label with ID ${operation.labelId} not found.`);
        }
        return new OperationHandlerCommand(this.modelState, this.createLabelEdit(sourceElement, operation), undefined);
    }

    /**
     * Creates the label edit workspace edit for the given AST node.
     *
     * @param node the AST node corresponding to the label
     * @returns the workspace edit to apply the label change, or undefined if not applicable
     */
    abstract createLabelEdit(node: AstNode, operation: ApplyLabelEditOperationType): WorkspaceEdit | undefined;

    /**
     * Creates a workspace edit for renaming the given leaf node to the new name.
     *
     * @param leafNode the leaf CST node to rename
     * @param newName the new name for the leaf node
     * @returns the workspace edit to perform the rename, or undefined if no references found
     */
    protected createRenameWorkspaceEdit(leafNode: CstNode, newName: string): WorkspaceEdit | undefined {
        const referencesService = this.modelState.languageServices.references.References;
        const targetNodes = referencesService.findDeclarations(leafNode);
        if (targetNodes.length === 0) {
            return undefined;
        }

        const targetNode = targetNodes[0];
        const options = { onlyLocal: false, includeDeclaration: true };
        const references = referencesService.findReferences(targetNode, options);
        const changes: Record<string, TextEditType[]> = {};
        const actualNewName = this.modelState.languageServices.AstSerializer.serializePrimitive({ value: newName }, ID);
        for (const ref of references) {
            const change = TextEdit.replace(ref.segment.range, actualNewName);
            const uri = ref.sourceUri.toString();
            if (changes[uri]) {
                changes[uri].push(change);
            } else {
                changes[uri] = [change];
            }
        }
        return { changes };
    }
}
