import type { Command } from "@eclipse-glsp/server";
import type { WorkspaceEdit } from "vscode-languageserver-types";
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
     * The edge type identifier.
     */
    edgeType: string;
    /**
     * Workspace edit to apply to the source model.
     */
    workspaceEdit: WorkspaceEdit;
    /**
     * Insert specification describing what was added to the model.
     * Used together with {@link insertedElement} to compute the edge id
     * automatically via the model id provider.
     */
    insertSpecification: InsertSpecification;
    /**
     * Metadata for the inserted AstNode. Used together with
     * {@link insertSpecification} for automatic id computation.
     */
    insertedElement: InsertedElementMetadata;
}

/**
 * Base handler for create-edge operations.
 * Subclasses implement language-specific edge insertion and id derivation.
 * The edge id is computed automatically from the {@link CreateEdgeResult.insertSpecification}
 * and {@link CreateEdgeResult.insertedElement} via the injected {@link ModelIdProvider}.
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

        const insertedElement: InsertedElementMetadata =
            result.insertedElement.edge != undefined
                ? {
                      ...result.insertedElement,
                      edge: {
                          ...result.insertedElement.edge,
                          meta: {
                              routingPoints: operation.routingPoints ?? [],
                              sourceAnchor: operation.sourceAnchor,
                              targetAnchor: operation.targetAnchor
                          }
                      }
                  }
                : result.insertedElement;

        const metadata = computeInsertionMetadata(
            this.modelState.sourceModel!,
            this.idProvider,
            [result.insertSpecification],
            [insertedElement],
            this.modelState.metadata
        );
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
}
