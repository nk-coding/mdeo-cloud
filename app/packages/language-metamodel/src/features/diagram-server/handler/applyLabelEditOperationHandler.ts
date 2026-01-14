import { BaseApplyLabelEditOperationHandler, sharedImport } from "@mdeo/language-shared";
import { Class, ClassImport } from "../../../grammar/metamodelTypes.js";
import type { AstNode } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { ApplyLabelEditOperation } from "@eclipse-glsp/server";

const { injectable } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");

/**
 * Handler for applying label edit operations from the client.
 */
@injectable()
export class ApplyLabelEditOperationHandler extends BaseApplyLabelEditOperationHandler {
    override createLabelEdit(node: AstNode, operation: ApplyLabelEditOperation): WorkspaceEdit | undefined {
        if (this.reflection.isInstance(node, Class)) {
            const nameNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
            if (nameNode != undefined) {
                return this.createRenameWorkspaceEdit(nameNode, operation.text);
            }
        } else if (this.reflection.isInstance(node, ClassImport)) {
            const nameNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
            if (nameNode != undefined) {
                return this.createRenameWorkspaceEdit(nameNode, operation.text);
            }
        }
        return undefined;
    }
}
