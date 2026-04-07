import type { Command } from "@eclipse-glsp/server";
import type { PasteOperation } from "@eclipse-glsp/protocol";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { Point } from "@eclipse-glsp/protocol";
import {
    deserializeClipboardData,
    resolveUniqueNames,
    toAstNodeLike,
    CLIPBOARD_AST_FORMAT,
    CLIPBOARD_POSITION_FORMAT,
    PASTE_DEFAULT_OFFSET,
    type ClipboardEdgeMetadata,
    type ClipboardPositionData,
    type SerializedClipboardNode
} from "../../clipboard/clipboardAstSerializer.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";
import type { MetadataEdits } from "./operationHandlerCommand.js";
import { sharedImport } from "../../sharedImport.js";
import { ExistingNamesProvider } from "../existingNamesProvider.js";

const { injectable, inject } = sharedImport("inversify");
const { PasteOperation: PasteOperationKind } = sharedImport("@eclipse-glsp/protocol");

/**
 * Base operation handler for paste operations.
 *
 * <p>Deserializes clipboard data produced by {@link BaseRequestClipboardDataActionHandler},
 * generates unique names for all named nodes, updates internal references accordingly,
 * and delegates the actual insertion to language-specific subclasses.
 *
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link insertNodes} – creates workspace edits for inserting the deserialized nodes</li>
 * </ul>
 *
 * <p>The set of already-used names is obtained from the injected
 * {@link ExistingNamesProvider}, which can be rebound in the diagram module.
 */
@injectable()
export abstract class BasePasteOperationHandler extends BaseOperationHandler {
    readonly operationType = PasteOperationKind.KIND;

    @inject(ExistingNamesProvider)
    protected existingNamesProvider!: ExistingNamesProvider;

    /**
     * Creates a command for the paste operation by deserializing the clipboard
     * data, resolving unique names, converting to AST-node-like objects, and
     * delegating insertion to the subclass.
     *
     * @param operation - The paste operation with clipboard data.
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

        const astNodeLikes = clipboardData.nodes.map((node) => toAstNodeLike(node));
        const result = await this.insertNodes(
            astNodeLikes,
            clipboardData.nodes,
            operation,
            nodePositions,
            offsetEdgeData
        );

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
     * @param rawClipboardData - The raw clipboard data map from the paste operation.
     * @param renameMap - Map from original node names to their final names after
     *   uniqueness resolution.
     * @param mousePosition - The last known mouse position in diagram coordinates,
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
     * Creates workspace edits and optional metadata edits to insert the pasted
     * nodes into the target document.
     *
     * @param astNodeLikes - The deserialized AST-node-like objects ready for text serialization.
     *   These objects have the shape of AST nodes with correct {@code $type} and property
     *   values, but without Langium runtime fields.
     * @param serializedNodes - The original serialized clipboard nodes (useful for inspecting
     *   raw clipboard data if needed).
     * @param operation - The original paste operation, providing editor context.
     * @param offsetPositions - Map from final node name to its computed target position in
     *   diagram coordinates. Subclasses should use these to populate metadata edits so that
     *   pasted nodes appear near the mouse cursor. Keyed by the final node name after
     *   uniqueness resolution.
     * @returns An object containing the workspace edit and optional metadata edits,
     *   or {@code undefined} if no insertion is possible.
     */
    protected abstract insertNodes(
        astNodeLikes: Record<string, unknown>[],
        serializedNodes: SerializedClipboardNode[],
        operation: PasteOperation,
        offsetPositions: Map<string, Point>,
        offsetEdgeData: ClipboardEdgeMetadata[]
    ): Promise<PasteInsertionResult | undefined>;
}

/**
 * Result of inserting pasted nodes into the document.
 */
export interface PasteInsertionResult {
    /** The workspace edit that inserts the serialized node text into the document. */
    workspaceEdit: WorkspaceEdit;
    /** Optional metadata edits for the newly created nodes (positions, types, etc.). */
    metadataEdits?: MetadataEdits;
}
