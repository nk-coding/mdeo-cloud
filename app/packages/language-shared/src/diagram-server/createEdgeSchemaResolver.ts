import type { CreateEdgeSchema } from "@mdeo/protocol-common";

/**
 * Request payload for resolving the initial create-edge schema.
 */
export interface InitialCreateEdgeSchemaRequest {
    sourceElementId: string;
    context?: unknown;
}

/**
 * Request payload for resolving a target-specific create-edge schema.
 */
export interface TargetCreateEdgeSchemaRequest {
    sourceElementId: string;
    targetElementId: string;
    context?: unknown;
}

/**
 * Backend resolver used by diagram-server action handlers to resolve
 * create-edge schemas requested by the client tool.
 */
export abstract class CreateEdgeSchemaResolver {
    /**
     * Resolves the initial schema for a selected source node.
     */
    abstract getInitialSchema(request: InitialCreateEdgeSchemaRequest): Promise<CreateEdgeSchema | undefined>;

    /**
     * Resolves a target-specific schema update.
     */
    abstract getTargetSchema(request: TargetCreateEdgeSchemaRequest): Promise<CreateEdgeSchema | undefined>;
}
