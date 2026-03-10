import { BaseApplyLabelEditOperationHandler, parseIdentifier, sharedImport } from "@mdeo/language-shared";
import { ObjectInstance, PropertyAssignment } from "../../../grammar/modelTypes.js";
import type { PartialPropertyAssignment } from "../../../grammar/modelPartialTypes.js";
import type { AstNode } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { ApplyLabelEditOperation } from "@eclipse-glsp/server";
import { ID } from "@mdeo/language-common";
import { parseModelPropertyLabel } from "../modelLabelEditValidator.js";

const { injectable } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");

/**
 * Handler for applying label edit operations from the client.
 * Handles renaming objects, changing types, and updating property values.
 */
@injectable()
export class ModelApplyLabelEditOperationHandler extends BaseApplyLabelEditOperationHandler {
    /**
     * Creates a workspace edit for the label change.
     *
     * @param node The AST node being edited
     * @param operation The apply label edit operation
     * @returns The workspace edit or undefined
     */
    override async createLabelEdit(
        node: AstNode,
        operation: ApplyLabelEditOperation
    ): Promise<WorkspaceEdit | undefined> {
        if (this.reflection.isInstance(node, ObjectInstance)) {
            return await this.createObjectLabelEdit(node, operation.text);
        } else if (this.reflection.isInstance(node, PropertyAssignment)) {
            return await this.createPropertyValueEdit(node, operation.text);
        }
        return undefined;
    }

    /**
     * Creates a workspace edit for updating an object's name and/or type.
     *
     * @param node The ObjectInstance node
     * @param text The new label text (format: "name : type")
     * @returns The workspace edit or undefined
     */
    private async createObjectLabelEdit(node: AstNode, text: string): Promise<WorkspaceEdit | undefined> {
        const parsed = this.parseObjectLabel(text);
        if (parsed == undefined) {
            return undefined;
        }

        const edits: WorkspaceEdit[] = [];

        const nameNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
        if (nameNode != undefined) {
            const parsedName = parseIdentifier(parsed.name);
            const renameEdit = this.createRenameWorkspaceEdit(nameNode, parsedName);
            if (renameEdit != undefined) {
                edits.push(renameEdit);
            }
        }

        const typeEdit = await this.createObjectTypeEdit(node, parsed.type);
        if (typeEdit != undefined) {
            edits.push(typeEdit);
        }

        return this.mergeWorkspaceEdits(edits);
    }

    /**
     * Creates a workspace edit for changing the object's type (class reference).
     * This changes which class the object is an instance of.
     *
     * @param node The ObjectInstance node
     * @param typeName The new type name
     * @returns The workspace edit or undefined
     */
    private async createObjectTypeEdit(node: AstNode, typeName: string): Promise<WorkspaceEdit | undefined> {
        const classNode = GrammarUtils.findNodeForProperty(node.$cstNode, "class");
        if (classNode == undefined) {
            return undefined;
        }

        const parsedType = parseIdentifier(typeName);
        return await this.replaceCstNode(classNode, parsedType);
    }

    /**
     * Parses an object label into name and type parts.
     *
     * @param label The label text (format: "name : type")
     * @returns Parsed name and type, or undefined if invalid
     */
    private parseObjectLabel(label: string): { name: string; type: string } | undefined {
        const colonIndex = label.lastIndexOf(":");
        if (colonIndex === -1) {
            return undefined;
        }

        const name = label.substring(0, colonIndex).trim();
        const type = label.substring(colonIndex + 1).trim();

        if (name.length === 0 || type.length === 0) {
            return undefined;
        }

        return { name, type };
    }

    /**
     * Creates a workspace edit for updating a property's name and/or value.
     * This changes which property is being assigned (not a rename operation).
     *
     * @param node The PropertyAssignment node
     * @param text The edited label text (format: "propName = value")
     * @returns The workspace edit or undefined
     */
    private async createPropertyValueEdit(
        node: PartialPropertyAssignment,
        text: string
    ): Promise<WorkspaceEdit | undefined> {
        if (text.trim().length === 0) {
            if (node.$cstNode == undefined) {
                return undefined;
            }
            return this.deleteCstNode(node.$cstNode);
        }

        const parsed = parseModelPropertyLabel(text, this.modelState.languageServices.parser.Lexer);
        if (typeof parsed === "string") {
            return undefined;
        }

        const edits: WorkspaceEdit[] = [];

        const nameNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
        if (nameNode != undefined) {
            const parsedName = parseIdentifier(parsed.name);
            const serializer = this.modelState.languageServices.AstSerializer;
            const serializedName = serializer.serializePrimitive({ value: parsedName }, ID);
            const nameEdit = await this.replaceCstNode(nameNode, serializedName);
            if (nameEdit != undefined) {
                edits.push(nameEdit);
            }
        }

        const valueNode = GrammarUtils.findNodeForProperty(node.$cstNode, "value");
        if (valueNode != undefined) {
            const valueEdit = await this.replaceCstNode(valueNode, parsed.value);
            if (valueEdit != undefined) {
                edits.push(valueEdit);
            }
        }

        return this.mergeWorkspaceEdits(edits);
    }
}
