import type { Command } from "@eclipse-glsp/server";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { EdgeMetadata } from "../metadata.js";
import type { InsertSpecification } from "../modelIdInsert.js";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";
import type { CreateEdgeOperation } from "@mdeo/protocol-common";
import { GNode } from "../model/node.js";
import { computeInsertionMetadata, type InsertedElementMetadata } from "./insertionMetadataHelper.js";
import type { ModelIdProvider } from "../modelIdProvider.js";
import { ModelIdProvider as ModelIdProviderKey } from "../modelIdProvider.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Result of a create-edge operation.
 */
export interface CreateEdgeResult {
    /**
     * The ID of the newly created edge.
     * Only required when not using the automatic id computation via
     * {@link insertSpecifications} and {@link insertedElements}.
     */
    edgeId?: string;
    /**
     * The edge type identifier.
     */
    edgeType: string;
    /**
     * Workspace edit to apply to the source model.
     */
    workspaceEdit: WorkspaceEdit;
    /**
     * Optional insert specifications describing what was added to the model.
     * When provided together with {@link insertedElements}, ids are computed
     * automatically via the model id provider instead of using the manual {@link edgeId}.
     */
    insertSpecifications?: InsertSpecification[];
    /**
     * Metadata for each inserted AstNode. When provided, the base handler
     * resolves id computation and builds metadata edits automatically.
     */
    insertedElements?: InsertedElementMetadata[];
}

/**
 * Base handler for create-edge operations.
 * Subclasses implement language-specific edge insertion and ID derivation.
 *
 * <p>Supports two modes:
 * <ul>
 *   <li><b>Manual ids</b>: the subclass provides a {@link CreateEdgeResult.edgeId} directly.</li>
 *   <li><b>Automatic ids</b>: the subclass provides {@link CreateEdgeResult.insertSpecifications}
 *       and {@link CreateEdgeResult.insertedElements}, and the base handler computes ids
 *       via the injected {@link ModelIdProvider}.</li>
 * </ul>
 */
@injectable()
export abstract class BaseCreateEdgeOperationHandler extends BaseOperationHandler {
    @inject(ModelIdProviderKey)
    protected idProvider!: ModelIdProvider;

    override async createCommand(operation: CreateEdgeOperation): Promise<Command | undefined> {
        const sourceElement = this.modelState.index.find(operation.sourceElementId);
        const targetElement = this.modelState.index.find(operation.targetElementId);

        if (!(sourceElement instanceof GNode) || !(targetElement instanceof GNode)) {
            throw new Error("Create-edge operation requires source and target nodes.");
        }

        const result = await this.createEdge(operation, sourceElement, targetElement);
        if (!result) {
            return undefined;
        }

        if (result.insertSpecifications != undefined && result.insertedElements != undefined) {
            const metadata = computeInsertionMetadata(
                this.modelState.sourceModel!,
                this.idProvider,
                result.insertSpecifications,
                result.insertedElements,
                this.modelState.metadata
            );
            return new OperationHandlerCommand(this.modelState, result.workspaceEdit, metadata);
        }

        const metadata = this.buildEdgeMetadata(operation, result);
        return new OperationHandlerCommand(this.modelState, result.workspaceEdit, metadata);
    }

    /**
     * Creates a language-specific edge insertion edit.
     *
     * @param operation The incoming create-edge operation
     * @param sourceElement The resolved source node
     * @param targetElement The resolved target node
     * @returns Language-specific create-edge result or undefined to abort handling
     */
    protected abstract createEdge(
        operation: CreateEdgeOperation,
        sourceElement: GNode,
        targetElement: GNode
    ): Promise<CreateEdgeResult | undefined>;

    /**
     * Builds metadata for the created edge.
     *
     * @param operation The original create-edge operation
     * @param result The result returned by language-specific creation
     * @returns Partial edge metadata keyed by the new edge ID
     */
    protected buildEdgeMetadata(
        operation: CreateEdgeOperation,
        result: CreateEdgeResult
    ): { edges: Record<string, Partial<EdgeMetadata>> } {
        return {
            edges: {
                [result.edgeId!]: {
                    from: operation.sourceElementId,
                    to: operation.targetElementId,
                    type: result.edgeType,
                    meta: {
                        routingPoints: operation.routingPoints ?? [],
                        sourceAnchor: operation.sourceAnchor,
                        targetAnchor: operation.targetAnchor
                    }
                }
            }
        };
    }
}
