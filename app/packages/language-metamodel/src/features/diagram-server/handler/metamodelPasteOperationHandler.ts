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
import { MetamodelElementType } from "@mdeo/protocol-metamodel";
import { Association, Class, Enum, type ClassType } from "../../../grammar/metamodelTypes.js";

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
        const insertSpecs: InsertSpecification[] = [
            {
                container: sourceModel,
                property: "elements",
                elements: astNodeLikes.map((n) => n as unknown as AstNode)
            }
        ];

        const insertedElements: InsertedElementMetadata[] = [];
        for (let i = 0; i < astNodeLikes.length; i++) {
            const nodeLike = astNodeLikes[i];
            const $type = serializedNodes[i].$type;
            if ($type === Class.name || $type === Enum.name) {
                const name = nodeLike.name as string | undefined;
                const position = name ? offsetPositions.get(name) : undefined;
                const elementType =
                    $type === Class.name ? MetamodelElementType.NODE_CLASS : MetamodelElementType.NODE_ENUM;
                if (position) {
                    insertedElements.push({
                        element: nodeLike as unknown as AstNode,
                        node: { type: elementType, meta: { position } }
                    });
                }
            } else if ($type === Association.name) {
                const edgeEntry = this.findMatchingEdgeEntry(nodeLike, offsetEdgeData);
                if (edgeEntry) {
                    const srcClassNode = this.resolveClassByName(edgeEntry.sourceClass, astNodeLikes, sourceModel);
                    const tgtClassNode = this.resolveClassByName(edgeEntry.targetClass, astNodeLikes, sourceModel);
                    if (srcClassNode && tgtClassNode) {
                        insertedElements.push({
                            element: nodeLike as unknown as AstNode,
                            edge: {
                                type: MetamodelElementType.EDGE_ASSOCIATION,
                                from: srcClassNode,
                                to: tgtClassNode,
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

        return {
            workspaceEdit: this.mergeWorkspaceEdits(edits),
            metadataEdits: computeInsertionMetadata(
                sourceModel,
                this.idProvider,
                insertSpecs,
                insertedElements,
                this.modelState.metadata
            )
        };
    }

    /**
     * Finds the {@link ClipboardEdgeMetadata} entry that corresponds to a pasted
     * association node-like by matching source and target class names and property names.
     *
     * @param associationLike The deserialized association node-like object.
     * @param edgeData The list of clipboard edge metadata entries.
     * @returns The matching entry, or `undefined` if none is found.
     */
    private findMatchingEdgeEntry(
        associationLike: Record<string, unknown>,
        edgeData: ClipboardEdgeMetadata[]
    ): ClipboardEdgeMetadata | undefined {
        const src = associationLike.source as Record<string, unknown> | undefined;
        const tgt = associationLike.target as Record<string, unknown> | undefined;
        const srcClass = (src?.class as { $refText?: string } | undefined)?.$refText;
        const tgtClass = (tgt?.class as { $refText?: string } | undefined)?.$refText;
        const srcProp = src?.name as string | undefined;
        const tgtProp = tgt?.name as string | undefined;
        return edgeData.find(
            (e) =>
                e.sourceClass === srcClass &&
                e.targetClass === tgtClass &&
                e.sourceProperty === srcProp &&
                e.targetProperty === tgtProp
        );
    }

    /**
     * Resolves a class AstNode by name, checking first among the node-likes being pasted
     * (for co-pasted classes), then in the existing source model elements.
     *
     * @param className The class name to find.
     * @param pastedNodeLikes The node-like objects being co-pasted in the same operation.
     * @param sourceModel The current model root.
     * @returns The matching {@link ClassType} AstNode, or `undefined` if not found.
     */
    private resolveClassByName(
        className: string,
        pastedNodeLikes: Record<string, unknown>[],
        sourceModel: AstNode
    ): ClassType | undefined {
        const pasted = pastedNodeLikes.find((n) => n.$type === Class.name && n.name === className);
        if (pasted) {
            return pasted as unknown as ClassType;
        }
        const elements = (sourceModel as unknown as Record<string, unknown>).elements as AstNode[] | undefined;
        return elements?.find((e: unknown) => {
            const r = e as Record<string, unknown>;
            return r.$type === Class.name && r.name === className;
        }) as ClassType | undefined;
    }
}
