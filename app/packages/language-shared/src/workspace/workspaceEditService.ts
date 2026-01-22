import type { AstSerializer, AstSerializerAdditionalServices, WorkspaceEditService } from "@mdeo/language-common";
import type { AstNode, CstNode, LangiumCoreServices, LangiumDocument } from "langium";
import type { Position, WorkspaceEdit } from "vscode-languageserver-types";
import { sharedImport } from "../sharedImport.js";

const { Range } = sharedImport("vscode-languageserver-types");

/**
 * Default implementation of the WorkspaceEditService.
 * Provides utility methods for creating workspace edits.
 */
export class DefaultWorkspaceEditService implements WorkspaceEditService {
    /**
     * The AST serializer used for serializing AST nodes.
     */
    private readonly serializer: AstSerializer;

    constructor(services: LangiumCoreServices & AstSerializerAdditionalServices) {
        this.serializer = services.AstSerializer;
    }

    mergeWorkspaceEdits(edits: WorkspaceEdit[]): WorkspaceEdit {
        const mergedEdit: WorkspaceEdit = { changes: {} };
        for (const edit of edits) {
            if (edit.changes) {
                for (const uri of Object.keys(edit.changes)) {
                    if (!mergedEdit.changes![uri]) {
                        mergedEdit.changes![uri] = [];
                    }
                    mergedEdit.changes![uri]!.push(...edit.changes[uri]!);
                }
            }
        }
        return mergedEdit;
    }

    deleteCstNode(cstNode: CstNode, documentUri: string): WorkspaceEdit {
        const edit: WorkspaceEdit = {
            changes: {
                [documentUri]: [
                    {
                        range: cstNode.range,
                        newText: ""
                    }
                ]
            }
        };
        return edit;
    }

    async replaceCstNode(
        cstNode: CstNode,
        newContent: AstNode | string,
        document: LangiumDocument
    ): Promise<WorkspaceEdit> {
        let replacementText: string;
        if (typeof newContent === "string") {
            replacementText = newContent;
        } else {
            replacementText = await this.serializeNode(newContent, document);
        }
        const indentation = this.getIndentationForLine(cstNode.range.start, document);
        const indentedText = this.applyIndentation(replacementText, indentation);
        const edit: WorkspaceEdit = {
            changes: {
                [document.uri.toString()]: [
                    {
                        range: cstNode.range,
                        newText: indentedText
                    }
                ]
            }
        };
        return edit;
    }

    getIndentationForLine(position: Position, document: LangiumDocument): string {
        const line = document.textDocument.getText(Range.create(position.line, 0, position.line + 1, 0));
        const match = line.match(/^\s*/);
        return match ? match[0] : "";
    }

    async createInsertAfterNodeEdit(
        cstNode: CstNode,
        content: string,
        document: LangiumDocument,
        ensureNewlineBefore: boolean = true
    ): Promise<WorkspaceEdit> {
        const text = document.textDocument.getText();
        const insertPosition = cstNode.range.end;

        const indentation = this.getIndentationForLine(cstNode.range.start, document);
        const indentedContent = this.applyIndentation(content, indentation);

        const prefix = this.calculateNewlinePrefix(text, insertPosition, document, ensureNewlineBefore);

        const edit: WorkspaceEdit = {
            changes: {
                [document.uri.toString()]: [
                    {
                        range: Range.create(insertPosition, insertPosition),
                        newText: prefix + indentedContent
                    }
                ]
            }
        };

        return edit;
    }

    async serializeNode(node: AstNode, document: LangiumDocument): Promise<string> {
        return await this.serializer.serializeNode(node, document, this.serializer.guessFormattingOptions(document));
    }

    private applyIndentation(content: string, indentation: string): string {
        return content
            .split("\n")
            .map((line, index) => (index === 0 ? line : indentation + line))
            .join("\n");
    }

    private calculateNewlinePrefix(
        text: string,
        insertPosition: Position,
        document: LangiumDocument,
        ensureNewlineBefore: boolean
    ): string {
        if (!ensureNewlineBefore || text.length === 0) {
            return "";
        }

        const textAfterNode = text.substring(document.textDocument.offsetAt(insertPosition));

        const startsWithNewline = textAfterNode.startsWith("\n");
        const startsWithDoubleNewline = textAfterNode.startsWith("\n\n");

        if (!startsWithNewline) {
            return "\n\n";
        } else if (!startsWithDoubleNewline) {
            return "\n";
        }
        return "";
    }
}
