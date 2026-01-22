import type { AstReflection, AstSerializer, WorkspaceEditService } from "@mdeo/language-common";
import { sharedImport } from "../../sharedImport.js";
import { AstReflectionKey } from "../langiumServices.js";
import type { GModelIndex } from "../modelIndex.js";
import type { ModelState } from "../modelState.js";
import type { Position, WorkspaceEdit } from "vscode-languageserver-types";
import type { AstNode, CstNode } from "langium";

const { injectable, inject } = sharedImport("inversify");
const { OperationHandler, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

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
     * Workspace edit service for creating workspace edits.
     */
    protected get workspaceEditService(): WorkspaceEditService {
        return this.modelState.languageServices.workspace.WorkspaceEdit;
    }

    /**
     * Gets the AST serializer service for serializing AST nodes.
     */
    protected get serializer(): AstSerializer {
        return this.modelState.languageServices.AstSerializer;
    }

    /**
     * Gets the source document for the current model state.
     *
     * @returns the source document
     * @throws Error if the source model document is not available
     */
    protected getSourceDocument() {
        const document = this.modelState.sourceModel?.$document;
        if (document == undefined) {
            throw new Error("Source model document is not available.");
        }
        return document;
    }

    /**
     * Merges multiple workspace edits into a single workspace edit.
     *
     * @param edits the array of workspace edits to merge
     * @returns the merged workspace edit
     */
    protected mergeWorkspaceEdits(edits: WorkspaceEdit[]): WorkspaceEdit {
        return this.workspaceEditService.mergeWorkspaceEdits(edits);
    }

    /**
     * Creates a workspace edit that deletes the text corresponding to the given CST node.
     *
     * @param cstNode the CST node to delete
     * @returns the workspace edit that performs the deletion
     */
    protected deleteCstNode(cstNode: CstNode): WorkspaceEdit {
        return this.workspaceEditService.deleteCstNode(cstNode, this.modelState.sourceUri!);
    }

    /**
     * Creates a workspace edit that replaces the text corresponding to the given CST node
     *
     * @param cstNode the CST node to replace
     * @param newContent the new content to insert (either as an AST node or a string)
     * @returns the workspace edit that performs the replacement
     */
    protected async replaceCstNode(cstNode: CstNode, newContent: AstNode | string): Promise<WorkspaceEdit> {
        const document = this.getSourceDocument();
        return this.workspaceEditService.replaceCstNode(cstNode, newContent, document);
    }

    /**
     * Serializes an AST node to a string.
     *
     * @param node the AST node to serialize
     * @returns the serialized string
     */
    protected async serializeNode(node: AstNode): Promise<string> {
        const document = this.getSourceDocument();
        return this.workspaceEditService.serializeNode(node, document);
    }

    /**
     * Retrieves the indentation string for the line at the given position.
     *
     * @param position the position to get the line indentation for
     * @returns the indentation string (spaces and/or tabs) for the line
     */
    protected getIndentationForLine(position: Position): string {
        const document = this.getSourceDocument();
        return this.workspaceEditService.getIndentationForLine(position, document);
    }

    /**
     * Creates a workspace edit to insert content after the given CST node.
     * Automatically adds proper spacing with blank lines if needed and applies proper indentation.
     *
     * @param cstNode the CST node after which to insert content
     * @param content the content to insert
     * @param ensureNewlineBefore whether to ensure a newline before the content (defaults to true, but not needed for empty documents)
     * @returns the workspace edit that performs the insertion
     */
    protected async createInsertAfterNodeEdit(
        cstNode: CstNode,
        content: string,
        ensureNewlineBefore: boolean = true
    ): Promise<WorkspaceEdit> {
        const document = this.getSourceDocument();
        return this.workspaceEditService.createInsertAfterNodeEdit(cstNode, content, document, ensureNewlineBefore);
    }
}
