import type { Action, MaybePromise } from "@eclipse-glsp/server";
import { type RequestClipboardDataAction, SetClipboardDataAction, type ClipboardData } from "@eclipse-glsp/protocol";
import type { AstNode } from "langium";
import { sharedImport } from "../../sharedImport.js";
import type { GModelIndex } from "../modelIndex.js";
import type { ModelState } from "../modelState.js";
import {
    serializeForClipboard,
    CLIPBOARD_AST_FORMAT,
    CLIPBOARD_POSITION_FORMAT,
    type ClipboardPositionData,
    type ClipboardEdgeMetadata
} from "../../clipboard/clipboardAstSerializer.js";
import type { NodeLayoutMetadata } from "@mdeo/protocol-common";

const { injectable, inject } = sharedImport("inversify");
const { GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Base action handler that responds to {@link RequestClipboardDataAction} by
 * serializing the selected AST nodes into a clipboard-compatible JSON format.
 *
 * <p>Subclasses must implement {@link getTopLevelAstNodes} to filter and
 * optionally transform the selected AST nodes for their specific language.
 * The base class handles ID-to-AST-node resolution, serialization, and
 * construction of the {@link SetClipboardDataAction} response.
 */
@injectable()
export abstract class BaseRequestClipboardDataActionHandler {
    /**
     * The action kind(s) this handler processes.
     */
    readonly actionKinds = ["requestClipboardData"];

    /**
     * The model state providing access to the source model.
     */
    @inject(sharedImport("@eclipse-glsp/server").ModelState)
    protected modelState!: ModelState;

    /**
     * The GModel index used to resolve graph element IDs to AST nodes.
     */
    @inject(GModelIndexKey)
    protected index!: GModelIndex;

    /**
     * Handles a {@link RequestClipboardDataAction} by looking up the selected
     * elements, extracting their AST nodes, serializing them, and returning
     * the serialized data as a {@link SetClipboardDataAction}.
     *
     * @param action The incoming request clipboard data action.
     * @returns An array containing a single {@link SetClipboardDataAction}.
     */
    execute(action: RequestClipboardDataAction): MaybePromise<Action[]> {
        const selectedIds = action.editorContext.selectedElementIds;
        const selectedAstNodes = this.resolveSelectedAstNodes(selectedIds);

        const positionData = this.buildClipboardPositionData(selectedAstNodes);

        const topLevelNodes = this.getTopLevelAstNodes(selectedAstNodes);
        if (topLevelNodes.length === 0) {
            const clipboardData: ClipboardData = {};
            return [SetClipboardDataAction.create(clipboardData, { responseId: action.requestId })];
        }

        const includedSet = new Set(topLevelNodes);
        const clipboardAstData = serializeForClipboard(topLevelNodes, includedSet);
        const jsonString = JSON.stringify(clipboardAstData);

        const clipboardData: ClipboardData = {
            [CLIPBOARD_AST_FORMAT]: jsonString,
            [CLIPBOARD_POSITION_FORMAT]: JSON.stringify(positionData)
        };

        return [SetClipboardDataAction.create(clipboardData, { responseId: action.requestId })];
    }

    /**
     * Returns edge layout metadata for any association edges in the selection.
     * Override in language-specific subclasses to collect edge routing data.
     *
     * @param _selectedAstNodes - The original selected AST nodes.
     * @returns An array of edge metadata entries, one per association edge.
     */
    protected getClipboardEdgeData(_selectedAstNodes: AstNode[]): ClipboardEdgeMetadata[] {
        return [];
    }

    /**
     * Builds clipboard position data by reading the current diagram positions
     * of the selected AST nodes from the model state metadata.
     *
     * <p>Only nodes that have a {@code name} property and a known element ID
     * in the current model index are included. Entries for nodes without a
     * recorded position are silently omitted.
     *
     * @param selectedAstNodes The original selected AST nodes (before any
     *   transformation by {@link getTopLevelAstNodes}).
     * @returns The clipboard position data object.
     */
    private buildClipboardPositionData(selectedAstNodes: AstNode[]): ClipboardPositionData {
        const positions: Record<string, { x: number; y: number }> = {};
        for (const node of selectedAstNodes) {
            const name = (node as { name?: string }).name;
            if (!name) {
                continue;
            }
            const elementId = this.index.getElementId(node);
            if (!elementId) {
                continue;
            }
            const nodeMeta = this.modelState.metadata.nodes[elementId];
            if (!nodeMeta) {
                continue;
            }
            const layoutMeta = nodeMeta.meta as NodeLayoutMetadata | undefined;
            if (layoutMeta?.position) {
                positions[name] = layoutMeta.position;
            }
        }
        const edges = this.getClipboardEdgeData(selectedAstNodes);
        return { positions, ...(edges.length > 0 ? { edges } : {}) };
    }

    /**
     * Resolves graph element IDs to their corresponding AST nodes using
     * the model index.
     *
     * @param selectedIds The IDs of the selected graph elements.
     * @returns An array of resolved AST nodes (elements without a mapping are skipped).
     */
    protected resolveSelectedAstNodes(selectedIds: string[]): AstNode[] {
        const astNodes: AstNode[] = [];
        for (const id of selectedIds) {
            const graphElement = this.index.get(id);
            if (graphElement) {
                const astNode = this.index.getAstNode(graphElement);
                if (astNode) {
                    astNodes.push(astNode);
                }
            }
        }
        return astNodes;
    }

    /**
     * Filters and optionally transforms the selected AST nodes into
     * the set of top-level nodes to include in the clipboard data.
     *
     * <p>Subclasses should implement this to:
     * <ul>
     *   <li>Filter to only supported top-level node types</li>
     *   <li>Deduplicate (e.g., skip child nodes whose parent is also selected)</li>
     *   <li>Transform nodes as needed (e.g., strip non-selected extensions from classes)</li>
     * </ul>
     *
     * @param selectedAstNodes The AST nodes corresponding to the selected graph elements.
     * @returns The filtered/transformed list of top-level AST nodes for clipboard serialization.
     */
    protected abstract getTopLevelAstNodes(selectedAstNodes: AstNode[]): AstNode[];
}
