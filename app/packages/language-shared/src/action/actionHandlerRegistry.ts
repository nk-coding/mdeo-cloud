import type {
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse
} from "@mdeo/language-common";

/**
 * Action handler that processes a specific action type.
 * Handlers can be stateful (store data between startAction and submitAction).
 */
export interface ActionHandler {
    /**
     * Starts the action dialog flow for this specific action type.
     *
     * @param params The action start parameters
     * @returns The first page of the action dialog
     */
    startAction(params: ActionStartParams): Promise<ActionStartResponse>;

    /**
     * Submits form data for the current action dialog page.
     *
     * @param params The submit parameters with collected inputs
     * @returns The submission response (validation errors, next page, or completion)
     */
    submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse>;
}

/**
 * Registry for managing action handlers.
 * Maps action types to their corresponding handler implementations.
 *
 * This follows the pattern used by Langium's ValidationRegistry:
 * - Handlers are registered by action type
 * - Lookup retrieves the handler for a specific action type
 * - Individual handlers encapsulate all logic for their action type
 */
export class ActionHandlerRegistry {
    private readonly handlers = new Map<string, ActionHandler>();

    /**
     * Registers an action handler for a specific action type.
     *
     * @param actionType The action type this handler processes
     * @param handler The handler implementation
     */
    register(actionType: string, handler: ActionHandler): void {
        if (this.handlers.has(actionType)) {
            throw new Error(`Handler already registered for action type: ${actionType}`);
        }
        this.handlers.set(actionType, handler);
    }

    /**
     * Retrieves the handler for a specific action type.
     *
     * @param actionType The action type to look up
     * @returns The handler if registered, undefined otherwise
     */
    getHandler(actionType: string): ActionHandler | undefined {
        return this.handlers.get(actionType);
    }

    /**
     * Checks if a handler is registered for the given action type.
     *
     * @param actionType The action type to check
     * @returns True if a handler is registered
     */
    hasHandler(actionType: string): boolean {
        return this.handlers.has(actionType);
    }

    /**
     * Gets all registered action types.
     *
     * @returns Array of registered action types
     */
    getRegisteredTypes(): string[] {
        return Array.from(this.handlers.keys());
    }
}
