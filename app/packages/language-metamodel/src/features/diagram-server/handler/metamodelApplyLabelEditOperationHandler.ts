import { BaseApplyLabelEditOperationHandler, parseIdentifier, sharedImport } from "@mdeo/language-shared";
import {
    Class,
    ClassImport,
    Property,
    type PropertyType,
    type SingleMultiplicityType,
    type RangeMultiplicityType,
    type MultiplicityType,
    AssociationEnd,
    type AssociationEndType
} from "../../../grammar/metamodelTypes.js";
import type { AstNode } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { ApplyLabelEditOperation } from "@eclipse-glsp/server";
import { parsePropertyLabel } from "../metamodelLabelEditValidator.js";

const { injectable } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");

/**
 * Handler for applying label edit operations from the client.
 */
@injectable()
export class MetamodelApplyLabelEditOperationHandler extends BaseApplyLabelEditOperationHandler {
    override async createLabelEdit(
        node: AstNode,
        operation: ApplyLabelEditOperation
    ): Promise<WorkspaceEdit | undefined> {
        if (this.reflection.isInstance(node, Class)) {
            const nameNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
            if (nameNode != undefined) {
                return this.createRenameWorkspaceEdit(nameNode, operation.text.trim());
            }
        } else if (this.reflection.isInstance(node, ClassImport)) {
            const nameNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
            if (nameNode != undefined) {
                return this.createRenameWorkspaceEdit(nameNode, operation.text.trim());
            }
        } else if (this.reflection.isInstance(node, Property)) {
            return await this.createRenamePropertyEdit(node, operation.text);
        } else if (this.reflection.isInstance(node, AssociationEnd)) {
            return await this.createRenameAssociationEndEdit(node, operation);
        }
        return undefined;
    }

    /**
     * Creates a workspace edit for renaming a property based on the edited label.
     *
     * @param node the property node
     * @param text the edited label text
     * @returns a workspace edit for renaming the property, or undefined if parsing fails
     */
    private async createRenamePropertyEdit(node: PropertyType, text: string): Promise<WorkspaceEdit | undefined> {
        const parsed = parsePropertyLabel(text);
        if (parsed == undefined) {
            return undefined;
        }

        const edits: WorkspaceEdit[] = [];

        const nameNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
        if (nameNode != undefined) {
            const renameEdit = this.createRenameWorkspaceEdit(nameNode, parseIdentifier(parsed.identifier));
            if (renameEdit != undefined) {
                edits.push(renameEdit);
            }
        }

        if (parsed.multiplicity != undefined) {
            const multiplicityAst = this.parseMultiplicity(parsed.multiplicity);
            if (multiplicityAst == undefined) {
                throw new Error("Invalid multiplicity format.");
            }

            if (node.multiplicity != undefined) {
                const multiplicityNode = GrammarUtils.findNodeForProperty(node.$cstNode, "multiplicity");
                if (multiplicityNode == undefined) {
                    throw new Error("Multiplicity CST node not found.");
                }
                edits.push(await this.replaceCstNode(multiplicityNode, multiplicityAst));
            } else {
                const typeNode = GrammarUtils.findNodeForProperty(node.$cstNode, "type");
                if (typeNode != undefined) {
                    const serializedMultiplicity = await this.serializeNode(multiplicityAst);
                    const newTypeText = typeNode.text + serializedMultiplicity;
                    edits.push(await this.replaceCstNode(typeNode, newTypeText));
                }
            }
        } else if (node.multiplicity != undefined) {
            const multiplicityNode = GrammarUtils.findNodeForProperty(node.$cstNode, "multiplicity");
            if (multiplicityNode != undefined) {
                edits.push(this.deleteCstNode(multiplicityNode));
            }
        }

        return this.mergeWorkspaceEdits(edits);
    }

    /**
     * Parses a multiplicity string and creates the appropriate AST node.
     *
     * Valid formats:
     * - Single: *, +, ?, or a number
     * - Range: number..*, or number..number
     *
     * @param multiplicity the multiplicity content string (without brackets)
     * @returns the parsed multiplicity AST node or undefined if invalid
     */
    private parseMultiplicity(multiplicity: string): MultiplicityType | undefined {
        if (multiplicity.includes("..")) {
            const parts = multiplicity.split("..");
            if (parts.length !== 2) {
                return undefined;
            }

            const lower = parts[0].trim();
            const upper = parts[1].trim();

            if (!/^\d+$/.test(lower)) {
                return undefined;
            }

            const lowerNum = parseInt(lower, 10);

            if (upper === "*") {
                return {
                    $type: "RangeMultiplicity",
                    lower: lowerNum,
                    upper: "*"
                } as RangeMultiplicityType;
            } else if (/^\d+$/.test(upper)) {
                return {
                    $type: "RangeMultiplicity",
                    lower: lowerNum,
                    upperNumeric: parseInt(upper, 10)
                } as RangeMultiplicityType;
            }

            return undefined;
        }

        if (multiplicity === "*" || multiplicity === "+" || multiplicity === "?") {
            return {
                $type: "SingleMultiplicity",
                value: multiplicity
            } as SingleMultiplicityType;
        }

        if (/^\d+$/.test(multiplicity)) {
            return {
                $type: "SingleMultiplicity",
                numericValue: parseInt(multiplicity, 10)
            } as SingleMultiplicityType;
        }

        return undefined;
    }

    /**
     * Creates a workspace edit for renaming an association end property or multiplicity.
     *
     * @param node The association node
     * @param operation The apply label edit operation
     * @returns A workspace edit for renaming the association end, or undefined if parsing fails
     */
    private async createRenameAssociationEndEdit(
        node: AssociationEndType,
        operation: ApplyLabelEditOperation
    ): Promise<WorkspaceEdit | undefined> {
        const labelId = operation.labelId;
        const text = operation.text.trim();

        if (labelId.endsWith("#property-label")) {
            const propertyNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
            if (propertyNode == undefined) {
                throw new Error("Property CST node not found.");
            }
            return this.createRenameWorkspaceEdit(propertyNode, parseIdentifier(text));
        } else if (labelId.endsWith("#multiplicity-label")) {
            return await this.updateAssociationMultiplicity(node, text);
        } else {
            throw new Error(`Unknown label ID type: ${labelId}`);
        }
    }

    /**
     * Updates the multiplicity of an association end.
     *
     * @param node The association node
     * @param end The end to update ("source" or "target")
     * @param multiplicityText The new multiplicity text
     * @returns A workspace edit for updating the multiplicity
     */
    private async updateAssociationMultiplicity(
        node: AssociationEndType,
        multiplicityText: string
    ): Promise<WorkspaceEdit | undefined> {
        const multiplicityAst = this.parseMultiplicity(multiplicityText);
        if (multiplicityAst == undefined) {
            throw new Error("Invalid multiplicity format.");
        }

        if (node.multiplicity != undefined) {
            const multiplicityNode = GrammarUtils.findNodeForProperty(node.$cstNode, "multiplicity");
            if (multiplicityNode == undefined) {
                throw new Error("Multiplicity CST node not found.");
            }
            return await this.replaceCstNode(multiplicityNode, multiplicityAst);
        } else {
            const serializedMultiplicity = await this.serializeNode(multiplicityAst);
            const insertPosition = node.$cstNode?.end;
            if (insertPosition == undefined) {
                throw new Error("Cannot determine insert position for multiplicity.");
            }
            return {
                changes: {
                    [node.$document!.uri.toString()]: [
                        {
                            range: {
                                start: node.$document!.textDocument.positionAt(insertPosition),
                                end: node.$document!.textDocument.positionAt(insertPosition)
                            },
                            newText: ` ${serializedMultiplicity}`
                        }
                    ]
                }
            };
        }

        return undefined;
    }
}
