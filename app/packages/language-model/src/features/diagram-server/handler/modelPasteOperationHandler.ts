import type { PasteOperation } from "@eclipse-glsp/protocol";
import type { Point } from "@eclipse-glsp/protocol";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { BasePasteOperationHandler, sharedImport, type PasteInsertionResult } from "@mdeo/language-shared";
import type { SerializedClipboardNode } from "@mdeo/language-shared";
import type { MetadataEdits } from "@mdeo/language-shared";
import type { ClipboardEdgeMetadata } from "@mdeo/language-shared";
import { ModelElementType } from "@mdeo/protocol-model";
import { ObjectInstance } from "../../../grammar/modelTypes.js";
import { ModelModelIdProvider } from "../modelModelIdProvider.js";

const { injectable } = sharedImport("inversify");


/**
 * Model-specific paste operation handler.
 *
 * <p>Inserts deserialized top-level model nodes (object instances, links) at
 * the end of the current document. For nodes that have a recorded position in
 * the clipboard data, metadata edits are emitted so the pasted elements appear
 * near the mouse cursor rather than at their original location.
 */
@injectable()
export class ModelPasteOperationHandler extends BasePasteOperationHandler {
    /**
     * Inserts all pasted nodes at the end of the model document and generates
     * metadata edits to position them near the paste cursor.
     *
     * <p>Each node is serialized to text via the AST serializer and appended
     * after the root CST node, with a blank line separating each insertion.
     *
     * @param astNodeLikes - The deserialized AST-node-like objects.
     * @param serializedNodes - The original serialized clipboard nodes.
     * @param _operation - The paste operation (unused).
     * @param offsetPositions - Map from final node name to its target position.
     * @returns The combined workspace edit and metadata edits, or {@code undefined}
     *   if no insertion is possible.
     */
    protected override async insertNodes(
        astNodeLikes: Record<string, unknown>[],
        serializedNodes: SerializedClipboardNode[],
        _operation: PasteOperation,
        offsetPositions: Map<string, Point>,
        _offsetEdgeData: ClipboardEdgeMetadata[]
    ): Promise<PasteInsertionResult | undefined> {
        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (!rootCstNode) {
            return undefined;
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        const edits: WorkspaceEdit[] = [];
        const metadataNodes: Record<string, { type: string; meta: object }> = {};

        for (let i = 0; i < astNodeLikes.length; i++) {
            const nodeLike = astNodeLikes[i];
            const serialized = await this.serializeNode(nodeLike as any);
            const edit = await this.createInsertAfterNodeEdit(rootCstNode, serialized, !isEmpty || edits.length > 0);
            edits.push(edit);

            const name = nodeLike.name as string | undefined;
            if (name && serializedNodes[i].$type === ObjectInstance.name) {
                const position = offsetPositions.get(name);
                if (position) {
                    const classRef = (nodeLike.class as { $refText?: string } | undefined)?.$refText ?? "";
                    const elementId = `${ObjectInstance.name}_${ModelModelIdProvider.escapeIdPart(classRef)}_${ModelModelIdProvider.escapeIdPart(name)}`;
                    metadataNodes[elementId] = { type: ModelElementType.NODE_OBJECT, meta: { position } };
                }
            }
        }

        if (edits.length === 0) {
            return undefined;
        }

        const metadataEdits: MetadataEdits | undefined =
            Object.keys(metadataNodes).length > 0 ? { nodes: metadataNodes } : undefined;

        return {
            workspaceEdit: this.mergeWorkspaceEdits(edits),
            metadataEdits
        };
    }
}
