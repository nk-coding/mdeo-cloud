import type { Action, Operation, EditorContext, Args } from "@eclipse-glsp/protocol";
import type { ContextActionItem } from "./contextActionsTypes.js";

/**
 * Operation to request context actions for selected element(s).
 *
 * Sent from the client to the server when the user requests context actions
 * for a single or multiple selected elements. This initiates a request-response
 * cycle where the server computes available context items and responds with
 * SetContextActionsAction.
 *
 * The operation enables dynamic, context-sensitive actions that appear
 * at specific attachment positions on nodes and edges, supporting operations like:
 * - Adding properties to classes
 * - Changing association end decorations
 * - Adding multiplicity indicators
 * - Inserting control flow statements
 * - And many other context-specific operations
 *
 * @see SetContextActionsAction - The response action
 * @see ContextActionItem - Individual action item structure
 */
export interface RequestContextActionsOperation extends Operation {
    /**
     * The operation kind identifier.
     *
     * This field uniquely identifies this operation type across the GLSP
     * protocol and is used for:
     * - Serialization and deserialization
     * - Operation handler registration and dispatch
     * - Protocol versioning and compatibility checks
     *
     * Must have the value "requestContextActions" for this operation type.
     */
    kind: "requestContextActions";

    /**
     * The context ID identifying which provider is responsible for actions.
     *
     * Used by the server to look up and invoke the correct ContextActionProvider
     * or ContextItemProvider implementation. Different context IDs can point to
     * different providers or configurations.
     *
     * Typical value: "element-context" for element-specific context actions
     *
     * Other possible values:
     * - "node-context" - For node-specific actions
     * - "edge-context" - For edge-specific actions
     * - "diagram-context" - For diagram-level actions
     * - Custom provider-defined IDs for specific element types or scenarios
     *
     * The provider is registered in the server's action handler registry
     * and is looked up by this contextId when handling the request.
     *
     * Examples:
     * - "element-context" - Standard element context provider
     * - "metamodel-context" - Metamodel-specific operations
     * - "transformation-context" - Model transformation specific actions
     */
    contextId: string;

    /**
     * The editor context containing selection and user data.
     *
     * Passed through from the client to provide context about the current
     * diagram state and user selection. Contains:
     *
     * - selectedElementIds: Array of selected element IDs (typically length 1
     *   for context actions, but can be multiple)
     * - args: Optional arguments that may influence available actions
     *   (user-provided data, metadata, flags, etc.)
     *
     * The handler uses this information to:
     * - Determine which elements the actions apply to
     * - Look up element metadata and types
     * - Apply user-provided filters or preferences
     * - Compute element-specific or context-specific actions
     *
     * @see EditorContext - GLSP protocol context structure
     */
    editorContext: EditorContext;
}

/**
 * Namespace containing RequestContextActionsOperation utilities.
 *
 * Provides factory functions and type guards for creating and working
 * with RequestContextActionsOperation instances.
 */
export namespace RequestContextActionsOperation {
    /**
     * The operation kind constant.
     *
     * Used for:
     * - Type checking and discrimination
     * - Handler registration
     * - Protocol versioning
     *
     * Value: "requestContextActions"
     */
    export const KIND = "requestContextActions";

    /**
     * Creates a RequestContextActionsOperation.
     *
     * Factory function that constructs a new operation with the specified
     * parameters. Simplifies operation creation by handling default values
     * and type safety.
     *
     * @param options The configuration for the operation
     * @param options.contextId The context provider identifier
     * @param options.selectedElementIds The ID(s) of selected elements
     * @param options.args Optional arguments for the provider
     * @returns A new RequestContextActionsOperation ready for dispatch
     *
     * @example
     * ```typescript
     * const operation = RequestContextActionsOperation.create({
     *   contextId: "element-context",
     *   selectedElementIds: ["Node_1"],
     *   args: { scope: "all" }
     * });
     * ```
     */
    export function create(options: {
        /**
         * The context provider identifier.
         */
        contextId: string;

        /**
         * The ID(s) of selected elements.
         */
        selectedElementIds: string[];

        /**
         * Optional arguments for the provider.
         */
        args?: Args;
    }): RequestContextActionsOperation {
        return {
            kind: KIND,
            isOperation: true,
            contextId: options.contextId,
            editorContext: {
                selectedElementIds: options.selectedElementIds,
                args: options.args
            }
        };
    }

    /**
     * Type guard to check if an Operation is a RequestContextActionsOperation.
     *
     * Used for type narrowing and runtime discrimination of operations.
     *
     * @param operation The operation to check
     * @returns true if the operation is a RequestContextActionsOperation, false otherwise
     *
     * @example
     * ```typescript
     * if (RequestContextActionsOperation.is(operation)) {
     *   const contextId = operation.contextId;
     * }
     * ```
     */
    export function is(operation: Operation): operation is RequestContextActionsOperation {
        return operation.kind === KIND;
    }
}

/**
 * Action sent from server to client containing computed context items.
 *
 * This is the response action to RequestContextActionsOperation. The server
 * computes available context actions for the selected element(s) and sends
 * back a list of ContextActionItems to be rendered in the UI.
 *
 * The action contains:
 * - The context ID (matching the request)
 * - The computed context items to display
 * - Positional and hierarchical information for rendering
 *
 * The client:
 * 1. Receives this action from the server
 * 2. Updates the context actions state
 * 3. Re-renders the diagram UI with new context buttons/menus
 * 4. Dispatches actions when user selects items
 *
 * Implementation Notes:
 * - This should typically be dispatched as a Command (not a regular action)
 *   to ensure proper diagram re-rendering and state updates
 * - The action should be handled by the ContextActionsManager or similar
 * - Client UI will convert items to visual elements based on position
 * - Nested items (children) are rendered as submenus or dropdowns
 *
 * @see RequestContextActionsOperation - The request that triggers this response
 * @see ContextActionItem - Individual action item structure
 */
export interface SetContextActionsAction extends Action {
    /**
     * The action kind identifier.
     *
     * This field uniquely identifies this action type and is used for:
     * - Action handler registration and dispatch
     * - Serialization and protocol compatibility
     * - Client-side action routing
     *
     * Must have the value "setContextActions" for this action type.
     */
    kind: "setContextActions";

    /**
     * The context ID these actions apply to.
     *
     * Should match the contextId from the corresponding RequestContextActionsOperation
     * to enable proper state management and action routing.
     *
     * Used by the client to:
     * - Associate actions with the correct context provider
     * - Route to the appropriate UI manager or handler
     * - Validate response matches request
     *
     * Examples: "element-context", "node-context", "edge-context"
     */
    contextId: string;

    /**
     * The context items to display.
     *
     * Array of ContextActionItems representing the available actions for
     * the selected element(s). Each item can be:
     *
     * - A leaf item: Has an action, executes when clicked
     * - A parent item: Has children, displays as submenu when activated
     * - An icon-only item: Has no label, renders compact
     * - A text-only item: Has no icon, renders as label only
     *
     * Items may include:
     * - Position information (EdgeAttachmentPosition) for edge rendering
     * - Icon and label for visual representation
     * - Nested children for hierarchical menus
     * - Sort order hints for consistent display
     *
     * The client renders items based on their properties:
     * - Icon: Rendered if present and icon is resolvable
     * - Label: Rendered next to icon if present
     * - Position: Used to place items at specific locations on edges
     * - Children: Rendered as submenu/dropdown if present
     *
     * Empty array means no context actions are available for the element.
     * This typically happens when:
     * - No context provider matches the element
     * - The element type doesn't support context actions
     * - All providers return empty results
     *
     * @see ContextActionItem - Individual item structure and properties
     */
    items: ContextActionItem[];
}

/**
 * Namespace containing SetContextActionsAction utilities.
 *
 * Provides factory functions and type guards for creating and working
 * with SetContextActionsAction instances.
 */
export namespace SetContextActionsAction {
    /**
     * The action kind constant.
     *
     * Used for:
     * - Type checking and discrimination
     * - Handler registration
     * - Protocol versioning
     *
     * Value: "setContextActions"
     */
    export const KIND = "setContextActions";

    /**
     * Creates a SetContextActionsAction.
     *
     * Factory function that constructs a new action with the specified
     * context items. Used by server-side handlers to create the response
     * action after computing available context items.
     *
     * @param options The configuration for the action
     * @param options.contextId The context provider identifier
     * @param options.items The context items to set
     * @returns A new SetContextActionsAction ready for dispatch to client
     *
     * @example
     * ```typescript
     * const action = SetContextActionsAction.create({
     *   contextId: "element-context",
     *   items: [
     *     {
     *       id: "add-property",
     *       label: "Add Property",
     *       icon: "plus",
     *       action: { kind: "addProperty", classId: "Node_1" }
     *     }
     *   ]
     * });
     * ```
     */
    export function create(options: {
        /**
         * The context provider identifier.
         */
        contextId: string;

        /**
         * The context items to set.
         */
        items: ContextActionItem[];
    }): SetContextActionsAction {
        return {
            kind: KIND,
            contextId: options.contextId,
            items: options.items
        };
    }

    /**
     * Type guard to check if an Action is a SetContextActionsAction.
     *
     * Used for type narrowing and runtime discrimination of actions.
     *
     * @param action The action to check
     * @returns true if the action is a SetContextActionsAction, false otherwise
     *
     * @example
     * ```typescript
     * if (SetContextActionsAction.is(action)) {
     *   const items = action.items;
     * }
     * ```
     */
    export function is(action: Action): action is SetContextActionsAction {
        return action.kind === KIND;
    }
}

/**
 * Helper function to create a RequestContextActionsOperation.
 *
 * Provides a convenient top-level factory function for creating
 * context action request operations.
 *
 * @param options The configuration for the operation
 * @param options.contextId The context provider identifier
 * @param options.selectedElementIds The ID(s) of selected elements
 * @param options.args Optional arguments for the provider
 * @returns A new RequestContextActionsOperation
 *
 * @example
 * ```typescript
 * const operation = createRequestContextActionsOperation({
 *   contextId: "element-context",
 *   selectedElementIds: ["Node_1"]
 * });
 * ```
 *
 * @see RequestContextActionsOperation.create - Equivalent namespace method
 */
export function createRequestContextActionsOperation(options: {
    /**
     * The context provider identifier.
     */
    contextId: string;

    /**
     * The ID(s) of selected elements.
     */
    selectedElementIds: string[];

    /**
     * Optional arguments for the provider.
     */
    args?: Args;
}): RequestContextActionsOperation {
    return RequestContextActionsOperation.create(options);
}

/**
 * Helper function to create a SetContextActionsAction.
 *
 * Provides a convenient top-level factory function for creating
 * context actions response actions.
 *
 * @param options The configuration for the action
 * @param options.contextId The context provider identifier
 * @param options.items The context items to set
 * @returns A new SetContextActionsAction
 *
 * @example
 * ```typescript
 * const action = createSetContextActionsAction({
 *   contextId: "element-context",
 *   items: availableItems
 * });
 * ```
 *
 * @see SetContextActionsAction.create - Equivalent namespace method
 */
export function createSetContextActionsAction(options: {
    /**
     * The context provider identifier.
     */
    contextId: string;

    /**
     * The context items to set.
     */
    items: ContextActionItem[];
}): SetContextActionsAction {
    return SetContextActionsAction.create(options);
}
