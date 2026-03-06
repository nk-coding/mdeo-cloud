import type { Action, RequestAction, ResponseAction } from "@eclipse-glsp/protocol";
import type { CreateEdgeSchema } from "../createEdgeSchema.js";

/**
 * Request action for the initial create-edge schema after selecting a source node.
 */
export interface RequestCreateEdgeInitialSchemaAction extends RequestAction<SetCreateEdgeInitialSchemaAction> {
    kind: typeof RequestCreateEdgeInitialSchemaAction.KIND;
    /**
     * Source element ID.
     */
    sourceElementId: string;
    /**
     * Optional client-provided context payload.
     */
    context?: unknown;
}

export namespace RequestCreateEdgeInitialSchemaAction {
    export const KIND = "requestCreateEdgeInitialSchema";

    export function create(options: {
        sourceElementId: string;
        context?: unknown;
        requestId?: string;
    }): RequestCreateEdgeInitialSchemaAction {
        return {
            kind: KIND,
            sourceElementId: options.sourceElementId,
            context: options.context,
            requestId: options.requestId ?? ""
        };
    }

    export function is(action: Action): action is RequestCreateEdgeInitialSchemaAction {
        return action.kind === KIND;
    }
}

/**
 * Response action containing the initial create-edge schema.
 */
export interface SetCreateEdgeInitialSchemaAction extends ResponseAction {
    kind: typeof SetCreateEdgeInitialSchemaAction.KIND;
    /**
     * Resolved schema, or undefined if edge creation cannot start.
     */
    schema?: CreateEdgeSchema;
}

export namespace SetCreateEdgeInitialSchemaAction {
    export const KIND = "setCreateEdgeInitialSchema";

    export function create(options: {
        schema?: CreateEdgeSchema;
        responseId?: string;
    }): SetCreateEdgeInitialSchemaAction {
        return {
            kind: KIND,
            schema: options.schema,
            responseId: options.responseId ?? ""
        };
    }

    export function is(action: Action): action is SetCreateEdgeInitialSchemaAction {
        return action.kind === KIND;
    }
}

/**
 * Request action for a target-specific create-edge schema update.
 */
export interface RequestCreateEdgeTargetSchemaAction extends RequestAction<SetCreateEdgeTargetSchemaAction> {
    kind: typeof RequestCreateEdgeTargetSchemaAction.KIND;
    /**
     * Source element ID.
     */
    sourceElementId: string;
    /**
     * Target element ID.
     */
    targetElementId: string;
    /**
     * Current schema at the client.
     */
    currentSchema: CreateEdgeSchema;
    /**
     * Optional client-provided context payload.
     */
    context?: unknown;
}

export namespace RequestCreateEdgeTargetSchemaAction {
    export const KIND = "requestCreateEdgeTargetSchema";

    export function create(options: {
        sourceElementId: string;
        targetElementId: string;
        currentSchema: CreateEdgeSchema;
        context?: unknown;
        requestId?: string;
    }): RequestCreateEdgeTargetSchemaAction {
        return {
            kind: KIND,
            sourceElementId: options.sourceElementId,
            targetElementId: options.targetElementId,
            currentSchema: options.currentSchema,
            context: options.context,
            requestId: options.requestId ?? ""
        };
    }

    export function is(action: Action): action is RequestCreateEdgeTargetSchemaAction {
        return action.kind === KIND;
    }
}

/**
 * Response action containing a target-specific schema update.
 */
export interface SetCreateEdgeTargetSchemaAction extends ResponseAction {
    kind: typeof SetCreateEdgeTargetSchemaAction.KIND;
    /**
     * Updated schema, or undefined if no update is available/valid.
     */
    schema?: CreateEdgeSchema;
}

export namespace SetCreateEdgeTargetSchemaAction {
    export const KIND = "setCreateEdgeTargetSchema";

    export function create(options: {
        schema?: CreateEdgeSchema;
        responseId?: string;
    }): SetCreateEdgeTargetSchemaAction {
        return {
            kind: KIND,
            schema: options.schema,
            responseId: options.responseId ?? ""
        };
    }

    export function is(action: Action): action is SetCreateEdgeTargetSchemaAction {
        return action.kind === KIND;
    }
}
