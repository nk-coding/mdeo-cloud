import type { PasteOperation } from "@eclipse-glsp/protocol";
import type { Point } from "@eclipse-glsp/protocol";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { AstNode } from "langium";
import {
    BasePasteOperationHandler,
    sharedImport,
    computeInsertionMetadata,
    type PasteInsertionResult,
    type InsertedElementMetadata,
    type InsertSpecification,
    type ReferenceResolutionContext,
    type ClipboardEdgeMetadata
} from "@mdeo/language-shared";
import { ModelElementType } from "@mdeo/protocol-model";
import { ObjectInstance, Link, type LinkType, type ObjectInstanceType } from "../../../grammar/modelTypes.js";

const { injectable } = sharedImport("inversify");

/**
 * Model-specific paste operation handler.
 *
 * <p>Inserts deserialized top-level model nodes (object instances, links) at
 * the end of the current document. Links whose source or target object reference
 * cannot be resolved are silently dropped.
 */
@injectable()
export class ModelPasteOperationHandler extends BasePasteOperationHandler {
    /**
     * Resolves a cross-reference by searching first among co-pasted nodes and
     * then in the existing source model elements. Only ObjectInstance nodes are
     * targets of cross-references in the model language.
     */
    protected override resolveReference(
        refText: string,
        { allPastedNodes }: ReferenceResolutionContext
    ): AstNode | undefined {
        const pasted = allPastedNodes.find(
            (n) => n.$type === ObjectInstance.name && (n as ObjectInstanceType).name === refText
        );
        if (pasted) {
            return pasted;
        }
        const objects = (this.modelState.sourceModel as unknown as { objects?: AstNode[] })?.objects ?? [];
        return objects.find((o) => o.$type === ObjectInstance.name && (o as ObjectInstanceType).name === refText);
    }

    /**
     * Validates a pasted node after reference resolution:
     * <ul>
     *   <li>{@link Link}: dropped when either end's object reference is unresolved.</li>
     *   <li>Everything else ({@link ObjectInstance}, etc.) is kept unchanged.</li>
     * </ul>
     */
    protected override validateNode(node: AstNode): AstNode | undefined {
        if (node.$type === Link.name) {
            const link = node as LinkType;
            if (!link.source.object.ref || !link.target.object.ref) {
                return undefined;
            }
        }
        return node;
    }

    /**
     * Inserts all pasted nodes at the end of the model document and generates
     * metadata edits to position them near the paste cursor.
     */
    protected override async insertNodes(
        astNodes: AstNode[],
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
        for (let i = 0; i < astNodes.length; i++) {
            const serialized = await this.serializeNode(astNodes[i] as any);
            const edit = await this.createInsertAfterNodeEdit(rootCstNode, serialized, !isEmpty || i > 0);
            edits.push(edit);
        }

        if (edits.length === 0) {
            return undefined;
        }

        const sourceModel = this.modelState.sourceModel!;
        const objectNodes = astNodes.filter((n) => n.$type === ObjectInstance.name);
        const linkNodes = astNodes.filter((n) => n.$type === Link.name);

        const insertSpecs: InsertSpecification[] = [];
        if (objectNodes.length > 0) {
            insertSpecs.push({ container: sourceModel, property: "objects", elements: objectNodes });
        }
        if (linkNodes.length > 0) {
            insertSpecs.push({ container: sourceModel, property: "links", elements: linkNodes });
        }

        const insertedElements: InsertedElementMetadata[] = [];
        for (const node of astNodes) {
            if (node.$type === ObjectInstance.name) {
                const name = (node as ObjectInstanceType).name;
                const position = name ? offsetPositions.get(name) : undefined;
                if (position) {
                    insertedElements.push(this.buildNodeElementMetadata(node, ModelElementType.NODE_OBJECT, position));
                }
            } else if (node.$type === Link.name) {
                const link = node as LinkType;
                // Both ends are guaranteed to be resolved by validateNode.
                const edgeMeta = this.findEdgeMetadata(
                    link.source.object.ref!.name,
                    link.source.property?.$refText ?? "",
                    link.target.object.ref!.name,
                    link.target.property?.$refText ?? "",
                    offsetEdgeData
                );
                if (edgeMeta) {
                    insertedElements.push(
                        this.buildEdgeElementMetadata(
                            node,
                            ModelElementType.EDGE_LINK,
                            link.source.object.ref! as AstNode,
                            link.target.object.ref! as AstNode,
                            edgeMeta
                        )
                    );
                }
            }
        }

        return {
            workspaceEdit: this.mergeWorkspaceEdits(edits),
            metadataEdits:
                insertSpecs.length > 0
                    ? computeInsertionMetadata(
                          sourceModel,
                          this.idProvider,
                          insertSpecs,
                          insertedElements,
                          this.modelState.metadata
                      )
                    : undefined
        };
    }
}
