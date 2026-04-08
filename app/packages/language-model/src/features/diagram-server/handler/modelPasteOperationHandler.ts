import type { PasteOperation } from "@eclipse-glsp/protocol";
import type { Point } from "@eclipse-glsp/protocol";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import {
    BasePasteOperationHandler,
    sharedImport,
    computeInsertionMetadata,
    type PasteInsertionResult,
    type InsertedElementMetadata,
    type InsertSpecification
} from "@mdeo/language-shared";
import type { SerializedClipboardNode } from "@mdeo/language-shared";
import type { ClipboardEdgeMetadata } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import { ModelElementType } from "@mdeo/protocol-model";
import { ObjectInstance, Link } from "../../../grammar/modelTypes.js";

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
        offsetEdgeData: ClipboardEdgeMetadata[]
    ): Promise<PasteInsertionResult | undefined> {
        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (!rootCstNode) {
            return undefined;
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        const edits: WorkspaceEdit[] = [];

        for (let i = 0; i < astNodeLikes.length; i++) {
            const nodeLike = astNodeLikes[i];
            const serialized = await this.serializeNode(nodeLike as any);
            const edit = await this.createInsertAfterNodeEdit(rootCstNode, serialized, !isEmpty || edits.length > 0);
            edits.push(edit);
        }

        if (edits.length === 0) {
            return undefined;
        }

        const sourceModel = this.modelState.sourceModel!;
        const objectNodeLikes: AstNode[] = [];
        const linkNodeLikes: AstNode[] = [];
        for (let i = 0; i < serializedNodes.length; i++) {
            const $type = serializedNodes[i].$type;
            if ($type === ObjectInstance.name) {
                objectNodeLikes.push(astNodeLikes[i] as unknown as AstNode);
            } else if ($type === Link.name) {
                linkNodeLikes.push(astNodeLikes[i] as unknown as AstNode);
            }
        }

        const insertSpecs: InsertSpecification[] = [];
        if (objectNodeLikes.length > 0) {
            insertSpecs.push({ container: sourceModel, property: "objects", elements: objectNodeLikes });
        }
        if (linkNodeLikes.length > 0) {
            insertSpecs.push({ container: sourceModel, property: "links", elements: linkNodeLikes });
        }

        const insertedElements: InsertedElementMetadata[] = [];
        for (let i = 0; i < astNodeLikes.length; i++) {
            if (serializedNodes[i].$type === ObjectInstance.name) {
                const name = astNodeLikes[i].name as string | undefined;
                const position = name ? offsetPositions.get(name) : undefined;
                if (position) {
                    insertedElements.push({
                        element: astNodeLikes[i] as unknown as AstNode,
                        node: { type: ModelElementType.NODE_OBJECT, meta: { position } }
                    });
                }
            } else if (serializedNodes[i].$type === Link.name) {
                const linkLike = astNodeLikes[i] as unknown as Record<string, unknown>;
                const edgeEntry = this.findMatchingEdgeEntry(linkLike, offsetEdgeData);
                if (edgeEntry) {
                    const sourceNode = this.resolveObjectByName(edgeEntry.sourceClass, objectNodeLikes, sourceModel);
                    const targetNode = this.resolveObjectByName(edgeEntry.targetClass, objectNodeLikes, sourceModel);
                    if (sourceNode && targetNode) {
                        insertedElements.push({
                            element: astNodeLikes[i] as unknown as AstNode,
                            edge: {
                                type: ModelElementType.EDGE_LINK,
                                from: sourceNode,
                                to: targetNode,
                                meta: {
                                    routingPoints: edgeEntry.routingPoints,
                                    ...(edgeEntry.sourceAnchor ? { sourceAnchor: edgeEntry.sourceAnchor } : {}),
                                    ...(edgeEntry.targetAnchor ? { targetAnchor: edgeEntry.targetAnchor } : {})
                                }
                            }
                        });
                    }
                }
            }
        }

        const metadataEdits =
            insertSpecs.length > 0
                ? computeInsertionMetadata(
                      sourceModel,
                      this.idProvider,
                      insertSpecs,
                      insertedElements,
                      this.modelState.metadata
                  )
                : undefined;

        return {
            workspaceEdit: this.mergeWorkspaceEdits(edits),
            metadataEdits
        };
    }

    private findMatchingEdgeEntry(
        linkLike: Record<string, unknown>,
        edgeData: ClipboardEdgeMetadata[]
    ): ClipboardEdgeMetadata | undefined {
        const src = linkLike.source as Record<string, unknown> | undefined;
        const tgt = linkLike.target as Record<string, unknown> | undefined;
        const srcName = (src?.object as { $refText?: string } | undefined)?.$refText;
        const tgtName = (tgt?.object as { $refText?: string } | undefined)?.$refText;
        const srcProp = (src?.property as { $refText?: string } | undefined)?.$refText ?? "";
        const tgtProp = (tgt?.property as { $refText?: string } | undefined)?.$refText ?? "";
        return edgeData.find(
            (e) =>
                e.sourceClass === srcName &&
                e.targetClass === tgtName &&
                e.sourceProperty === srcProp &&
                e.targetProperty === tgtProp
        );
    }

    private resolveObjectByName(name: string, pastedObjects: AstNode[], sourceModel: AstNode): AstNode | undefined {
        const pasted = pastedObjects.find((o) => (o as unknown as { name?: string }).name === name);
        if (pasted) return pasted;
        const objects = (sourceModel as unknown as Record<string, unknown>).objects as AstNode[] | undefined;
        return objects?.find((o) => (o as unknown as { name?: string }).name === name);
    }
}
