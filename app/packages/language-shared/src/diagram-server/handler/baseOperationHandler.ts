import type { AstReflection, AstSerializer } from "@mdeo/language-common";
import { sharedImport } from "../../sharedImport.js";
import { AstReflectionKey } from "../langiumServices.js";
import type { GModelIndex } from "../modelIndex.js";
import type { ModelState } from "../modelState.js";
import type { Position, WorkspaceEdit } from "vscode-languageserver-types";
import type { AstNode, CstNode } from "langium";

const { injectable, inject } = sharedImport("inversify");
const { OperationHandler, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");
const { Range } = sharedImport("vscode-languageserver-types");

/**
 * Base class for operation handlers in the diagram server.
 * Provides common functionality for handling diagram operations.
 */
@injectable()
export abstract class BaseOperationHandler extends OperationHandler {
    declare modelState: ModelState;

    /**
     * Injected model index for accessing graph model elements.
     */
    @inject(GModelIndexKey)
    protected index!: GModelIndex;

    /**
     * Injected AST reflection service for type checking and model introspection.
     */
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;

    /**
     * Gets the AST serializer service for serializing AST nodes.
     */
    protected get serializer(): AstSerializer {
        return this.modelState.languageServices.AstSerializer;
    }

    /**
     * Merges multiple workspace edits into a single workspace edit.
     *
     * @param edits the array of workspace edits to merge
     * @returns the merged workspace edit
     */
    protected mergeWorkspaceEdits(edits: WorkspaceEdit[]): WorkspaceEdit {
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

    /**
     * Creates a workspace edit that deletes the text corresponding to the given CST node.
     *
     * @param cstNode the CST node to delete
     * @returns the workspace edit that performs the deletion
     */
    protected deleteCstNode(cstNode: CstNode): WorkspaceEdit {
        const edit: WorkspaceEdit = {
            changes: {
                [this.modelState.sourceUri!]: [
                    {
                        range: cstNode.range,
                        newText: ""
                    }
                ]
            }
        };
        return edit;
    }

    /**
     * Creates a workspace edit that replaces the text corresponding to the given CST node
     *
     * @param cstNode the CST node to replace
     * @param newContent the new content to insert (either as an AST node or a string)
     * @returns the workspace edit that performs the replacement
     */
    protected async replaceCstNode(cstNode: CstNode, newContent: AstNode | string): Promise<WorkspaceEdit> {
        let replacementText: string;
        if (typeof newContent === "string") {
            replacementText = newContent;
        } else {
            replacementText = await this.serializeNode(newContent);
        }
        const indentation = this.getIndentationForLine(cstNode.range.start);
        const indentedText = replacementText
            .split("\n")
            .map((line, index) => (index === 0 ? line : indentation + line))
            .join("\n");
        const edit: WorkspaceEdit = {
            changes: {
                [this.modelState.sourceUri!]: [
                    {
                        range: cstNode.range,
                        newText: indentedText
                    }
                ]
            }
        };
        return edit;
    }

    /**
     * Serializes an AST node to a string.
     *
     * @param node the AST node to serialize
     * @returns the serialized string
     */
    protected async serializeNode(node: AstNode): Promise<string> {
        const serializer = this.modelState.languageServices.AstSerializer;
        const document = this.modelState.sourceModel?.$document;
        if (document == undefined) {
            throw new Error("Source model document is not available for serialization.");
        }
        return await serializer.serializeNode(node, document, serializer.guessFormattingOptions(document));
    }

    /**
     * Retrieves the indentation string for the line at the given position.
     *
     * @param position the position to get the line indentation for
     * @returns the indentation string (spaces and/or tabs) for the line
     */
    protected getIndentationForLine(position: Position): string {
        const document = this.modelState.sourceModel?.$document;
        if (document == undefined) {
            throw new Error("Source model document is not available for indentation retrieval.");
        }
        const line = document.textDocument.getText(Range.create(position.line, 0, position.line + 1, 0));
        const match = line.match(/^\s*/);
        return match ? match[0] : "";
    }
}
