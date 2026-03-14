import type { Command } from "@eclipse-glsp/server";
import type { CreateNodeOperation } from "@eclipse-glsp/protocol";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { NodeLayoutMetadata } from "@mdeo/protocol-common";
import type { NodeMetadata } from "../metadata.js";
import { sharedImport } from "../../sharedImport.js";
import { BaseOperationHandler } from "./baseOperationHandler.js";
import { OperationHandlerCommand } from "./operationHandlerCommand.js";

const { injectable } = sharedImport("inversify");

/**
 * Result of a create node operation.
 */
export interface CreateNodeResult {
    /**
     * The ID of the newly created node.
     */
    nodeId: string;
    /**
     * The type identifier for the node (used in metadata).
     */
    nodeType: string;
    /**
     * The workspace edit to apply to update the source model.
     */
    workspaceEdit: WorkspaceEdit;
}

/**
 * Base handler for applying create node operations from the client.
 * Subclasses must implement the createNode method to define
 * language-specific node creation behavior.
 */
@injectable()
export abstract class BaseCreateNodeOperationHandler extends BaseOperationHandler {
    override async createCommand(operation: CreateNodeOperation): Promise<Command | undefined> {
        const result = await this.createNode(operation);

        if (result == undefined) {
            return undefined;
        }

        const metadata = this.buildNodeMetadata(operation, result);

        return new OperationHandlerCommand(this.modelState, result.workspaceEdit, metadata);
    }

    /**
     * Creates the new node and returns the result with node ID, type, and workspace edit.
     * Subclasses must implement this method to define language-specific node creation logic.
     *
     * @param operation The create node operation
     * @returns The create node result containing node ID, type, and workspace edit, or undefined if not applicable
     */
    abstract createNode(operation: CreateNodeOperation): Promise<CreateNodeResult | undefined>;

    /**
     * Builds metadata for the newly created node.
     * Sets the node's position from the operation and includes type information.
     *
     * @param operation The create node operation
     * @param result The create node result
     * @returns Metadata object with node information
     */
    protected buildNodeMetadata(
        operation: CreateNodeOperation,
        result: CreateNodeResult
    ): { nodes: Record<string, Partial<NodeMetadata>> } {
        const layoutMetadata: NodeLayoutMetadata = {};

        if (operation.location) {
            layoutMetadata.position = operation.location;
        }

        return {
            nodes: {
                [result.nodeId]: {
                    type: result.nodeType,
                    meta: layoutMetadata
                }
            }
        };
    }

    /**
     * Finds a unique name by checking existing exported symbols in the source model.
     *
     * @param name The base name to check for uniqueness
     * @returns The unique name, potentially with a numerical suffix appended
     */
    protected async findUniqueName(name: string): Promise<string> {
        const document = this.modelState.sourceModel?.$document;
        if (!document) {
            return name;
        }
        const exported =
            await this.modelState.languageServices.references.ScopeComputation.collectExportedSymbols(document);
        const names = new Set(exported.map((description) => description.name));
        if (!names.has(name)) {
            return name;
        }
        let suffix = 1;
        while (names.has(`${name}${suffix}`)) {
            suffix++;
        }
        return name + suffix.toString();
    }
}
