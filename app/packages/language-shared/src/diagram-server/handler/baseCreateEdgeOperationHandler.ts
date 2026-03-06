import type { Command } from "@eclipse-glsp/server";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { EdgeMetadata } from "../metadata.js";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";
import type { CreateEdgeOperation } from "@mdeo/editor-protocol";
import { GNode } from "../model/node.js";

const { injectable } = sharedImport("inversify");

/**
 * Result of a create-edge operation.
 */
export interface CreateEdgeResult {
    /**
     * The ID of the newly created edge.
     */
    edgeId: string;
    /**
     * The edge type identifier.
     */
    edgeType: string;
    /**
     * Workspace edit to apply to the source model.
     */
    workspaceEdit: WorkspaceEdit;
}

/**
 * Base handler for create-edge operations.
 * Subclasses implement language-specific edge insertion and ID derivation.
 */
@injectable()
export abstract class BaseCreateEdgeOperationHandler extends BaseOperationHandler {
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
                [result.edgeId]: {
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
