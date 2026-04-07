import type { PasteOperation } from "@eclipse-glsp/protocol";
import type { Point } from "@eclipse-glsp/protocol";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { BasePasteOperationHandler, sharedImport, type PasteInsertionResult } from "@mdeo/language-shared";
import type { SerializedClipboardNode } from "@mdeo/language-shared";
import type { MetadataEdits } from "@mdeo/language-shared";
import type { ClipboardEdgeMetadata } from "@mdeo/language-shared";
import { MetamodelElementType } from "@mdeo/protocol-metamodel";
import { Class, Enum } from "../../../grammar/metamodelTypes.js";
import { MetamodelModelIdProvider } from "../metamodelModelIdProvider.js";

const { injectable } = sharedImport("inversify");

/**
 * Metamodel-specific paste operation handler.
 *
 * <p>Inserts deserialized top-level metamodel nodes (classes, enums, associations)
 * at the end of the current document. For nodes that have a recorded position in
 * the clipboard data, metadata edits are emitted so the pasted elements appear
 * near the mouse cursor rather than at their original location.
 */
@injectable()
export class MetamodelPasteOperationHandler extends BasePasteOperationHandler {
    /**
     * Inserts all pasted nodes at the end of the metamodel document and
     * generates metadata edits to position them near the paste cursor.
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
        offsetEdgeData: ClipboardEdgeMetadata[]
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

            // Build metadata edit for nodes that have a target position.
            const name = nodeLike.name as string | undefined;
            if (name) {
                const position = offsetPositions.get(name);
                if (position) {
                    const $type = serializedNodes[i].$type;
                    const elementId = `${$type}_${MetamodelModelIdProvider.escapeIdPart(name)}`;
                    const elementType =
                        $type === Class.name
                            ? MetamodelElementType.NODE_CLASS
                            : $type === Enum.name
                              ? MetamodelElementType.NODE_ENUM
                              : undefined;
                    if (elementType) {
                        metadataNodes[elementId] = { type: elementType, meta: { position } };
                    }
                }
            }
        }

        if (edits.length === 0) {
            return undefined;
        }

        // Build edge metadata edits for pasted associations.
        // offsetEdgeData entries already carry post-rename class names and offset routing points.
        const metadataEdges: Record<string, { type: string; from: string; to: string; meta: object }> = {};
        for (const edgeEntry of offsetEdgeData) {
            const srcEscaped = MetamodelModelIdProvider.escapeIdPart(edgeEntry.sourceClass);
            const tgtEscaped = MetamodelModelIdProvider.escapeIdPart(edgeEntry.targetClass);
            const srcPropEscaped = MetamodelModelIdProvider.escapeIdPart(edgeEntry.sourceProperty);
            const tgtPropEscaped = MetamodelModelIdProvider.escapeIdPart(edgeEntry.targetProperty);
            const edgeId = `Association_${srcEscaped}_${srcPropEscaped}_${tgtEscaped}_${tgtPropEscaped}`;
            const fromId = `Class_${srcEscaped}`;
            const toId = `Class_${tgtEscaped}`;
            metadataEdges[edgeId] = {
                type: MetamodelElementType.EDGE_ASSOCIATION,
                from: fromId,
                to: toId,
                meta: {
                    routingPoints: edgeEntry.routingPoints,
                    ...(edgeEntry.sourceAnchor ? { sourceAnchor: edgeEntry.sourceAnchor } : {}),
                    ...(edgeEntry.targetAnchor ? { targetAnchor: edgeEntry.targetAnchor } : {})
                }
            };
        }

        const hasNodeEdits = Object.keys(metadataNodes).length > 0;
        const hasEdgeEdits = Object.keys(metadataEdges).length > 0;
        const metadataEdits: MetadataEdits | undefined =
            hasNodeEdits || hasEdgeEdits
                ? {
                      nodes: hasNodeEdits ? metadataNodes : undefined,
                      edges: hasEdgeEdits ? metadataEdges : undefined
                  }
                : undefined;

        return {
            workspaceEdit: this.mergeWorkspaceEdits(edits),
            metadataEdits
        };
    }
}
