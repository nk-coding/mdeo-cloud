import type { Command } from "@eclipse-glsp/server";
import type { PasteOperation } from "@eclipse-glsp/protocol";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { Point } from "@eclipse-glsp/protocol";
import type { AstNode } from "langium";
import {
    deserializeClipboardData,
    resolveUniqueNames,
    toAstNodeLike,
    CLIPBOARD_AST_FORMAT,
    CLIPBOARD_POSITION_FORMAT,
    PASTE_DEFAULT_OFFSET,
    type ClipboardEdgeMetadata,
    type ClipboardPositionData
} from "../../clipboard/clipboardAstSerializer.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";
import type { MetadataEdits } from "./operationHandlerCommand.js";
import type { InsertedElementMetadata } from "./insertionMetadataHelper.js";
import { sharedImport } from "../../sharedImport.js";
import { ExistingNamesProvider } from "../existingNamesProvider.js";
import type { ModelIdProvider } from "../modelIdProvider.js";
import { ModelIdProvider as ModelIdProviderKey } from "../modelIdProvider.js";

const { injectable, inject } = sharedImport("inversify");
const { PasteOperation: PasteOperationKind } = sharedImport("@eclipse-glsp/protocol");

/**
 * Context provided to {@link BasePasteOperationHandler.resolveReference} for
 * each unresolved cross-reference encountered while preparing pasted nodes.
 */
export interface ReferenceResolutionContext {
    /**
     * The property name on the owner node that holds this reference.
     */
    readonly propertyName: string;
    /**
     * The immediate owner of this reference.
     */
    readonly ownerNode: AstNode;
    /**
     * All top-level nodes being pasted in the same operation (resolved refs already present).
     */
    readonly allPastedNodes: AstNode[];
}

/**
 * Base operation handler for paste operations.
 *
 * {@link ExistingNamesProvider}, which can be rebound in the diagram module.
 */
@injectable()
export abstract class BasePasteOperationHandler extends BaseOperationHandler {
    readonly operationType = PasteOperationKind.KIND;

    @inject(ExistingNamesProvider)
    protected existingNamesProvider!: ExistingNamesProvider;

    @inject(ModelIdProviderKey)
    protected idProvider!: ModelIdProvider;

    /**
     * Creates a command for the paste operation by deserializing the clipboard
     * data, resolving unique names, resolving cross-references, validating nodes,
     * and delegating insertion to the subclass.
     *
     * @param operation The paste operation with clipboard data.
     * @returns A command that applies the paste, or {@code undefined} if the
     *   clipboard data is invalid or empty.
     */
    override async createCommand(operation: PasteOperation): Promise<Command | undefined> {
        const clipboardJson = operation.clipboardData[CLIPBOARD_AST_FORMAT];
        if (!clipboardJson) {
            return undefined;
        }

        const clipboardData = deserializeClipboardData(clipboardJson);
        if (!clipboardData || clipboardData.nodes.length === 0) {
            return undefined;
        }

        const existingNames = await this.existingNamesProvider.getExistingNames();
        const renameMap = resolveUniqueNames(clipboardData, existingNames);

        const { nodePositions, edgeData: offsetEdgeData } = this.computeOffsets(
            operation.clipboardData,
            renameMap,
            operation.editorContext.lastMousePosition
        );

        const rawNodes = clipboardData.nodes.map((node) => toAstNodeLike(node) as unknown as AstNode);
        this.resolveAllRefs(rawNodes);
        const astNodes = rawNodes.flatMap((node) => {
            const validated = this.validateNode(node);
            return validated !== undefined ? [validated] : [];
        });

        if (astNodes.length === 0) {
            return undefined;
        }

        const result = await this.insertNodes(astNodes, operation, nodePositions, offsetEdgeData);

        if (!result) {
            return undefined;
        }

        return new OperationHandlerCommand(this.modelState, result.workspaceEdit, result.metadataEdits);
    }

    /**
     * Computes the target diagram positions for each pasted node and the offset
     * routing points for each pasted association edge.
     *
     * <p>The offset vector is determined so that the top-left corner of the entire
     * pasted group (nodes + edge routing points combined) lands at the current
     * mouse position. When no mouse position is available, a small fixed offset is
     * applied to avoid exact overlap with the originals.
     *
     * <p>For edges, the original class names in each {@link ClipboardEdgeMetadata}
     * entry are updated to the post-rename names using the provided
     * {@code renameMap}, so that callers can directly derive the new element ID.
     *
     * @param rawClipboardData The raw clipboard data map from the paste operation.
     * @param renameMap Map from original node names to their final names after
     *   uniqueness resolution.
     * @param mousePosition The last known mouse position in diagram coordinates,
     *   or {@code undefined} when not available.
     * @returns Node positions keyed by final name, and edge data with offset routing
     *   points and renamed class names.
     */
    private computeOffsets(
        rawClipboardData: Record<string, string | undefined>,
        renameMap: Map<string, string>,
        mousePosition: Point | undefined
    ): { nodePositions: Map<string, Point>; edgeData: ClipboardEdgeMetadata[] } {
        const raw = rawClipboardData[CLIPBOARD_POSITION_FORMAT];
        if (!raw) {
            return { nodePositions: new Map(), edgeData: [] };
        }

        let posData: ClipboardPositionData;
        try {
            posData = JSON.parse(raw) as ClipboardPositionData;
        } catch {
            return { nodePositions: new Map(), edgeData: [] };
        }

        const nodeEntries = Object.entries(posData.positions);
        const edgeEntries = posData.edges ?? [];

        const allPoints: Point[] = [...nodeEntries.map(([, p]) => p), ...edgeEntries.flatMap((e) => e.routingPoints)];

        if (allPoints.length === 0) {
            return { nodePositions: new Map(), edgeData: [] };
        }

        let offset: Point;
        if (mousePosition) {
            const topLeft = {
                x: Math.min(...allPoints.map((p) => p.x)),
                y: Math.min(...allPoints.map((p) => p.y))
            };
            offset = { x: mousePosition.x - topLeft.x, y: mousePosition.y - topLeft.y };
        } else {
            offset = { x: PASTE_DEFAULT_OFFSET, y: PASTE_DEFAULT_OFFSET };
        }

        const nodePositions = new Map<string, Point>();
        for (const [originalName, pos] of nodeEntries) {
            const newName = renameMap.get(originalName) ?? originalName;
            nodePositions.set(newName, { x: pos.x + offset.x, y: pos.y + offset.y });
        }

        const edgeData: ClipboardEdgeMetadata[] = edgeEntries.map((entry) => ({
            ...entry,
            sourceClass: renameMap.get(entry.sourceClass) ?? entry.sourceClass,
            targetClass: renameMap.get(entry.targetClass) ?? entry.targetClass,
            routingPoints: entry.routingPoints.map((p) => ({ x: p.x + offset.x, y: p.y + offset.y }))
        }));

        return { nodePositions, edgeData };
    }

    /**
     * Walks all pasted nodes and resolves any unresolved cross-references
     * ({@code ref: undefined}) by calling the abstract {@link resolveReference}.
     * References that cannot be resolved are left with {@code ref: undefined}
     * and a valid {@code $refText} so the serializer can still produce text.
     */
    private resolveAllRefs(allPastedNodes: AstNode[]): void {
        for (const node of allPastedNodes) {
            this.resolveRefsInNode(node, allPastedNodes);
        }
    }

    private resolveRefsInNode(node: AstNode, allPastedNodes: AstNode[]): void {
        for (const [key, value] of Object.entries(node)) {
            if (key.startsWith("$")) {
                continue;
            }
            this.resolveRefsInValue(value, key, node, allPastedNodes);
        }
    }

    private resolveRefsInValue(
        value: unknown,
        propertyName: string,
        ownerNode: AstNode,
        allPastedNodes: AstNode[]
    ): void {
        if (value === null || value === undefined) {
            return;
        }
        if (Array.isArray(value)) {
            for (const item of value) {
                this.resolveRefsInValue(item, propertyName, ownerNode, allPastedNodes);
            }
            return;
        }
        if (typeof value === "object") {
            const obj = value as Record<string, unknown>;
            if ("$refText" in obj && "ref" in obj && obj.ref === undefined) {
                const refText = obj.$refText as string;
                const resolved = this.resolveReference(refText, { propertyName, ownerNode, allPastedNodes });
                if (resolved !== undefined) {
                    obj.ref = resolved;
                }
            } else if ("$type" in obj) {
                this.resolveRefsInNode(obj as unknown as AstNode, allPastedNodes);
            } else {
                for (const [k, v] of Object.entries(obj)) {
                    this.resolveRefsInValue(v, k, ownerNode, allPastedNodes);
                }
            }
        }
    }

    /**
     * Attempts to resolve a single cross-reference by name and context.
     *
     * <p>Implementations should look up the target first among
     * {@link ReferenceResolutionContext.allPastedNodes} (for co-pasted elements)
     * and then in the current source model. Returning {@code undefined} leaves
     * the reference with only a {@code $refText}, which is tolerated for
     * optional references (e.g. enum-type properties in a class) but should
     * cause {@link validateNode} to reject edges whose required ends are missing.
     *
     * @param refText The reference text (name) to resolve.
     * @param context Contextual information about where the reference appears.
     * @returns The resolved AST node, or {@code undefined} if not found.
     */
    protected abstract resolveReference(refText: string, context: ReferenceResolutionContext): AstNode | undefined;

    /**
     * Validates and optionally transforms a single pasted AST node after
     * reference resolution.
     *
     * @param node The resolved AST node to validate.
     * @returns The node to include (possibly modified), or {@code undefined} to skip it.
     */
    protected validateNode(node: AstNode): AstNode | undefined {
        return node;
    }

    /**
     * Creates workspace edits and optional metadata edits to insert the pasted
     * nodes into the target document.
     *
     * <p>All nodes passed here have already had their cross-references resolved
     * (to the extent possible) and have passed {@link validateNode}. Edges are
     * guaranteed to have both ends resolved; their {@code .ref} fields are safe
     * to access without additional null-checks.
     *
     * @param astNodes The prepared AST nodes ready for serialization and insertion.
     * @param operation The original paste operation, providing editor context.
     * @param offsetPositions Map from final node name to its computed target
     *   position in diagram coordinates.
     * @param offsetEdgeData Edge routing metadata with renamed and offset routing points.
     * @returns An object containing the workspace edit and optional metadata edits,
     *   or {@code undefined} if no insertion is possible.
     */
    protected abstract insertNodes(
        astNodes: AstNode[],
        operation: PasteOperation,
        offsetPositions: Map<string, Point>,
        offsetEdgeData: ClipboardEdgeMetadata[]
    ): Promise<PasteInsertionResult | undefined>;

    /**
     * Finds the {@link ClipboardEdgeMetadata} entry that matches the given source
     * and target endpoint identifiers.
     *
     * @param sourceEndName The name of the source end (object / class name).
     * @param sourceEndProperty The property name on the source end (empty string if none).
     * @param targetEndName The name of the target end.
     * @param targetEndProperty The property name on the target end (empty string if none).
     * @param offsetEdgeData The list of clipboard edge metadata entries to search.
     * @returns The matching entry, or {@code undefined} if none is found.
     */
    protected findEdgeMetadata(
        sourceEndName: string,
        sourceEndProperty: string,
        targetEndName: string,
        targetEndProperty: string,
        offsetEdgeData: ClipboardEdgeMetadata[]
    ): ClipboardEdgeMetadata | undefined {
        return offsetEdgeData.find(
            (e) =>
                e.sourceClass === sourceEndName &&
                e.targetClass === targetEndName &&
                e.sourceProperty === sourceEndProperty &&
                e.targetProperty === targetEndProperty
        );
    }

    /**
     * Builds an {@link InsertedElementMetadata} descriptor for a diagram node
     * (non-edge) element.
     *
     * @param element The AST node being inserted.
     * @param type The diagram element type identifier.
     * @param position The target position in diagram coordinates.
     * @returns The metadata descriptor.
     */
    protected buildNodeElementMetadata(element: AstNode, type: string, position: Point): InsertedElementMetadata {
        return { element, node: { type, meta: { position } } };
    }

    /**
     * Builds an {@link InsertedElementMetadata} descriptor for a diagram edge element.
     *
     * @param element The AST node being inserted.
     * @param type The diagram element type identifier.
     * @param from The AST node of the source end.
     * @param to The AST node of the target end.
     * @param edgeMeta The clipboard edge metadata carrying routing points and anchors.
     * @returns The metadata descriptor.
     */
    protected buildEdgeElementMetadata(
        element: AstNode,
        type: string,
        from: AstNode,
        to: AstNode,
        edgeMeta: ClipboardEdgeMetadata
    ): InsertedElementMetadata {
        return {
            element,
            edge: {
                type,
                from,
                to,
                meta: {
                    routingPoints: edgeMeta.routingPoints,
                    ...(edgeMeta.sourceAnchor ? { sourceAnchor: edgeMeta.sourceAnchor } : {}),
                    ...(edgeMeta.targetAnchor ? { targetAnchor: edgeMeta.targetAnchor } : {})
                }
            }
        };
    }
}

/**
 * Result of inserting pasted nodes into the document.
 */
export interface PasteInsertionResult {
    /**
     * The workspace edit that inserts the serialized node text into the document.
     */
    workspaceEdit: WorkspaceEdit;
    /**
     * Optional metadata edits for the newly created nodes (positions, types, etc.).
     */
    metadataEdits?: MetadataEdits;
}
