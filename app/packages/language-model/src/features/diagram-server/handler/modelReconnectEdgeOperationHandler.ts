import type { GEdge } from "@mdeo/language-shared";
import {
    BaseReconnectEdgeOperationHandler,
    sharedImport,
    DefaultModelIdRegistry,
    ModelIdProvider as ModelIdProviderKey,
    type ModelIdProvider,
    type ModelIdRegistry,
    type ReconnectEndpoints,
    type ReconnectEdgeResult
} from "@mdeo/language-shared";
import type { ReconnectEdgeOperation } from "@mdeo/editor-protocol";
import type { AstNode } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { Link } from "../../../grammar/modelTypes.js";
import type { PartialLink, PartialObjectInstance } from "../../../grammar/modelPartialTypes.js";
import { ModelElementType } from "../model/elementTypes.js";

const { injectable, inject } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");

/**
 * Handler for applying reconnect edge operations from the client.
 * Handles reconnecting link edges between different objects.
 */
@injectable()
export class ModelReconnectEdgeOperationHandler extends BaseReconnectEdgeOperationHandler {
    @inject(ModelIdProviderKey)
    protected idProvider!: ModelIdProvider;
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

        const sourceModel = this.modelState.sourceModel;
        if (sourceModel == undefined) return undefined;
        const registry = new DefaultModelIdRegistry(sourceModel, this.idProvider);

        if (sourceChanged && targetChanged) {
            return await this.handleBothEndsChanged(link, endpoints, registry);
        } else if (sourceChanged) {
            return await this.handleSourceChanged(link, endpoints, registry);
        } else if (targetChanged) {
            return await this.handleTargetChanged(link, endpoints, registry);
        }

        return undefined;
    }

    /**
     * Handles case where both source and target changed.
     *
     * @param link The Link AST node
     * @param endpoints The old and new endpoints
     * @param registry The model ID registry for consistent name lookup
     * @returns The reconnect result
     */
    private async handleBothEndsChanged(
        link: PartialLink,
        endpoints: ReconnectEndpoints,
        registry: ModelIdRegistry
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

        const newEdgeId = this.computeNewEdgeId(
            link,
            newSourceNode as PartialObjectInstance,
            newTargetNode as PartialObjectInstance,
            registry
        );

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
     * @param registry The model ID registry for consistent name lookup
     * @returns The reconnect result
     */
    private async handleSourceChanged(
        link: PartialLink,
        endpoints: ReconnectEndpoints,
        registry: ModelIdRegistry
    ): Promise<ReconnectEdgeResult | undefined> {
        const newSourceNode = this.index.getAstNode(endpoints.newSource);

        if (newSourceNode == undefined) {
            return undefined;
        }

        const edit = await this.createLinkEndEdit(link, "source", newSourceNode);
        if (edit == undefined) {
            return undefined;
        }

        const newEdgeId = this.computeNewEdgeId(link, newSourceNode as PartialObjectInstance, undefined, registry);

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
     * @param registry The model ID registry for consistent name lookup
     * @returns The reconnect result
     */
    private async handleTargetChanged(
        link: PartialLink,
        endpoints: ReconnectEndpoints,
        registry: ModelIdRegistry
    ): Promise<ReconnectEdgeResult | undefined> {
        const newTargetNode = this.index.getAstNode(endpoints.newTarget);

        if (newTargetNode == undefined) {
            return undefined;
        }

        const edit = await this.createLinkEndEdit(link, "target", newTargetNode);
        if (edit == undefined) {
            return undefined;
        }

        const newEdgeId = this.computeNewEdgeId(link, undefined, newTargetNode as PartialObjectInstance, registry);

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
     * Computes the new edge ID after reconnection, consistent with the server's
     * ModelModelIdProvider link ID format: Link_{objectName}[_{property}]--{objectName}[_{property}].
     *
     * Uses the registry to look up instance names, falling back to direct
     * {@link PartialObjectInstance.name} access. For unchanged ends, the existing
     * link end's resolved object is used.
     *
     * @param link The Link AST node
     * @param newSource The new source object instance (undefined if source unchanged)
     * @param newTarget The new target object instance (undefined if target unchanged)
     * @param registry The model ID registry for consistent name lookup
     * @returns The new edge ID
     */
    private computeNewEdgeId(
        link: PartialLink,
        newSource: PartialObjectInstance | undefined,
        newTarget: PartialObjectInstance | undefined,
        registry: ModelIdRegistry
    ): string {
        const sourceObj = newSource ?? (link.source?.object?.ref as PartialObjectInstance | undefined);
        const targetObj = newTarget ?? (link.target?.object?.ref as PartialObjectInstance | undefined);

        const sourceName =
            (sourceObj != undefined ? (registry.getName(sourceObj as AstNode) ?? sourceObj.name) : undefined) ??
            "unnamed";
        const targetName =
            (targetObj != undefined ? (registry.getName(targetObj as AstNode) ?? targetObj.name) : undefined) ??
            "unnamed";

        const sourceProp = link.source?.property?.$refText ?? "";
        const targetProp = link.target?.property?.$refText ?? "";
        const sourceEnd = sourceProp ? `${sourceName}_${sourceProp}` : sourceName;
        const targetEnd = targetProp ? `${targetName}_${targetProp}` : targetName;

        return `${Link.name}_${sourceEnd}--${targetEnd}`;
    }
}
