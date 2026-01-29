import type { GEdge } from "@mdeo/language-shared";
import {
    BaseReconnectEdgeOperationHandler,
    sharedImport,
    type ReconnectEndpoints,
    type ReconnectEdgeResult
} from "@mdeo/language-shared";
import type { ReconnectEdgeOperation } from "@mdeo/editor-protocol";
import type { AstNode } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { Link } from "../../../grammar/modelTypes.js";
import type { PartialLink, PartialObjectInstance } from "../../../grammar/modelPartialTypes.js";
import { ModelElementType } from "../model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");

/**
 * Handler for applying reconnect edge operations from the client.
 * Handles reconnecting link edges between different objects.
 */
@injectable()
export class ModelReconnectEdgeOperationHandler extends BaseReconnectEdgeOperationHandler {
    /**
     * Creates a reconnect edit for a link edge.
     *
     * @param node The Link AST node
     * @param operation The reconnect edge operation
     * @param edge The GEdge being reconnected
     * @param endpoints The old and new source/target endpoints
     * @returns The reconnect result or undefined
     */
    override async createReconnectEdit(
        node: AstNode,
        operation: ReconnectEdgeOperation,
        edge: GEdge,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        if (edge.type === ModelElementType.EDGE_LINK) {
            return await this.handleLinkEdgeReconnect(node, operation, endpoints);
        }
        return undefined;
    }

    /**
     * Handles reconnection of link edges.
     *
     * @param node The Link AST node
     * @param operation The reconnect edge operation
     * @param endpoints The old and new source/target nodes
     * @returns The reconnect result with workspace edit and new edge ID
     */
    private async handleLinkEdgeReconnect(
        node: AstNode,
        operation: ReconnectEdgeOperation,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        if (!this.reflection.isInstance(node, Link)) {
            return undefined;
        }

        const link = node as PartialLink;
        const sourceChanged = endpoints.oldSource.id !== endpoints.newSource.id;
        const targetChanged = endpoints.oldTarget.id !== endpoints.newTarget.id;

        if (sourceChanged && targetChanged) {
            return await this.handleBothEndsChanged(link, endpoints);
        } else if (sourceChanged) {
            return await this.handleSourceChanged(link, endpoints);
        } else if (targetChanged) {
            return await this.handleTargetChanged(link, endpoints);
        }

        return undefined;
    }

    /**
     * Handles case where both source and target changed.
     *
     * @param link The Link AST node
     * @param endpoints The old and new endpoints
     * @returns The reconnect result
     */
    private async handleBothEndsChanged(
        link: PartialLink,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        const edits: WorkspaceEdit[] = [];

        const newSourceNode = this.index.getAstNode(endpoints.newSource);
        const newTargetNode = this.index.getAstNode(endpoints.newTarget);

        if (newSourceNode == undefined || newTargetNode == undefined) {
            return undefined;
        }

        const sourceEdit = await this.createLinkEndEdit(link, "source", newSourceNode);
        const targetEdit = await this.createLinkEndEdit(link, "target", newTargetNode);

        if (sourceEdit) edits.push(sourceEdit);
        if (targetEdit) edits.push(targetEdit);

        const newEdgeId = this.calculateNewEdgeId(endpoints.newSource.id, endpoints.newTarget.id);

        return {
            newEdgeId,
            workspaceEdit: this.mergeWorkspaceEdits(edits)
        };
    }

    /**
     * Handles case where only the source changed.
     *
     * @param link The Link AST node
     * @param endpoints The old and new endpoints
     * @returns The reconnect result
     */
    private async handleSourceChanged(
        link: PartialLink,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        const newSourceNode = this.index.getAstNode(endpoints.newSource);

        if (newSourceNode == undefined) {
            return undefined;
        }

        const edit = await this.createLinkEndEdit(link, "source", newSourceNode);
        if (edit == undefined) {
            return undefined;
        }

        const newEdgeId = this.calculateNewEdgeId(endpoints.newSource.id, endpoints.newTarget.id);

        return {
            newEdgeId,
            workspaceEdit: edit
        };
    }

    /**
     * Handles case where only the target changed.
     *
     * @param link The Link AST node
     * @param endpoints The old and new endpoints
     * @returns The reconnect result
     */
    private async handleTargetChanged(
        link: PartialLink,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        const newTargetNode = this.index.getAstNode(endpoints.newTarget);

        if (newTargetNode == undefined) {
            return undefined;
        }

        const edit = await this.createLinkEndEdit(link, "target", newTargetNode);
        if (edit == undefined) {
            return undefined;
        }

        const newEdgeId = this.calculateNewEdgeId(endpoints.newSource.id, endpoints.newTarget.id);

        return {
            newEdgeId,
            workspaceEdit: edit
        };
    }

    /**
     * Creates a workspace edit to update a link end reference.
     *
     * @param link The Link AST node
     * @param endType Whether this is the source or target end
     * @param newObject The new object to reference
     * @returns The workspace edit or undefined
     */
    private async createLinkEndEdit(
        link: PartialLink,
        endType: "source" | "target",
        newObject: AstNode
    ): Promise<WorkspaceEdit | undefined> {
        const linkEnd = endType === "source" ? link.source : link.target;
        if (linkEnd == undefined) {
            return undefined;
        }

        const objectRefNode = GrammarUtils.findNodeForProperty(linkEnd.$cstNode, "object");
        if (objectRefNode == undefined) {
            return undefined;
        }

        const newObjectInstance = newObject as PartialObjectInstance;
        const newRefText = newObjectInstance.name ?? "unknown";

        return await this.replaceCstNode(objectRefNode, newRefText);
    }

    /**
     * Calculates the new edge ID after reconnection.
     *
     * @param sourceId The new source node ID
     * @param targetId The new target node ID
     * @returns The new edge ID
     */
    private calculateNewEdgeId(sourceId: string, targetId: string): string {
        return `${sourceId}--${targetId}`;
    }
}
