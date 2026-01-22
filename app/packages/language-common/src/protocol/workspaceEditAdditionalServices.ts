import type { AstNode, CstNode, LangiumDocument } from "langium";
import type { WorkspaceEdit, Position } from "vscode-languageserver-types";

/**
 * Service for creating workspace edits based on AST and CST nodes.
 * Provides utility methods for inserting, replacing, and deleting content in documents.
 */
export interface WorkspaceEditService {
    /**
     * Merges multiple workspace edits into a single workspace edit.
     *
     * @param edits the array of workspace edits to merge
     * @returns the merged workspace edit
     */
    mergeWorkspaceEdits(edits: WorkspaceEdit[]): WorkspaceEdit;

    /**
     * Creates a workspace edit that deletes the text corresponding to the given CST node.
     *
     * @param cstNode the CST node to delete
     * @param documentUri the URI of the document to edit
     * @returns the workspace edit that performs the deletion
     */
    deleteCstNode(cstNode: CstNode, documentUri: string): WorkspaceEdit;

    /**
     * Creates a workspace edit that replaces the text corresponding to the given CST node.
     *
     * @param cstNode the CST node to replace
     * @param newContent the new content to insert (either as an AST node or a string)
     * @param document the document to edit
     * @returns the workspace edit that performs the replacement
     */
    replaceCstNode(cstNode: CstNode, newContent: AstNode | string, document: LangiumDocument): Promise<WorkspaceEdit>;

    /**
     * Retrieves the indentation string for the line at the given position.
     *
     * @param position the position to get the line indentation for
     * @param document the document to read from
     * @returns the indentation string (spaces and/or tabs) for the line
     */
    getIndentationForLine(position: Position, document: LangiumDocument): string;

    /**
     * Creates a workspace edit to insert content after the given CST node.
     * Automatically adds proper spacing with blank lines if needed and applies proper indentation.
     *
     * @param cstNode the CST node after which to insert content
     * @param content the content to insert
     * @param document the document to edit
     * @param ensureNewlineBefore whether to ensure a newline before the content (defaults to true)
     * @returns the workspace edit that performs the insertion
     */
    createInsertAfterNodeEdit(
        cstNode: CstNode,
        content: string,
        document: LangiumDocument,
        ensureNewlineBefore?: boolean
    ): Promise<WorkspaceEdit>;

    /**
     * Serializes an AST node to a string using the provided serializer.
     *
     * @param node the AST node to serialize
     * @param document the document containing the node
     * @returns the serialized string
     */
    serializeNode(node: AstNode, document: LangiumDocument): Promise<string>;
}

/**
 * Additional services interface for workspace edit functionality.
 */
export interface WorkspaceEditAdditionalServices {
    workspace: {
        WorkspaceEdit: WorkspaceEditService;
    };
}
