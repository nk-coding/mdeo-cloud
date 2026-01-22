import type { PluginContext } from "../plugin/pluginContext.js";

/**
 * Represents a serializable icon node.
 * Each element is a tuple of [tagName, attributes].
 * This is compatible with Lucide IconNode but uses simpler types for serialization.
 */
export type ActionIconNode = [string, Record<string, string>][];

/**
 * Primitive type forms supported by the action schema.
 * Based on a simplified JSON Type Definition (RFC 8927) without timestamp support.
 */
export type ActionSchemaPrimitiveType =
    | "int8"
    | "int16"
    | "int32"
    | "uint8"
    | "uint16"
    | "uint32"
    | "float32"
    | "float64"
    | "string"
    | "boolean";

/**
 * Schema form representing a primitive type.
 */
export interface ActionSchemaTypeForm {
    /**
     * The primitive type of this schema element
     */
    type: ActionSchemaPrimitiveType;
    /**
     * Optional placeholder text for the input field
     */
    placeholder?: string;
}

/**
 * Schema form representing an enumeration of string values.
 */
export interface ActionSchemaEnumForm {
    /**
     * The allowed string values for this enumeration
     */
    enum: string[];
    /**
     * If true, render as a combobox allowing custom input
     */
    combobox?: boolean;
    /**
     * Optional placeholder text for the input field
     */
    placeholder?: string;
}

/**
 * Schema form representing an array of elements.
 */
export interface ActionSchemaElementsForm {
    /**
     * The schema for array elements
     */
    elements: ActionSchema;
}

/**
 * Schema form representing an object with properties.
 */
export interface ActionSchemaPropertiesForm {
    /**
     * Required properties of the object
     */
    properties: Record<string, ActionSchema>;
    /**
     * Optional custom labels for the properties
     */
    propertyLabels?: Record<string, string>;
}

/**
 * Schema form representing an optional value.
 */
export interface ActionSchemaOptionalForm {
    /**
     * The schema for the optional value
     */
    optional: ActionSchema;
}

/**
 * A simplified JSON Type Definition schema for action dialog forms.
 * Supports type forms, enum forms, elements forms, properties forms, and optional forms.
 * Does not support nullable types or additional properties.
 */
export type ActionSchema =
    | ActionSchemaTypeForm
    | ActionSchemaEnumForm
    | ActionSchemaElementsForm
    | ActionSchemaPropertiesForm
    | ActionSchemaOptionalForm;

/**
 * Represents a single page in an action dialog.
 */
export interface ActionDialogPage {
    /**
     * The title displayed at the top of the dialog page
     */
    title: string;
    /**
     * A description rendered below the title
     */
    description: string;
    /**
     * The schema defining the form inputs for this page
     */
    schema: ActionSchema;
    /**
     * Whether this is the last page in the dialog flow
     */
    isLastPage: boolean;
    /**
     * Optional custom label for the Next/Submit button
     */
    submitButtonLabel?: string;
}

/**
 * Parameters for starting an action dialog.
 */
export interface ActionStartParams {
    /**
     * The type of event that triggered the action dialog (e.g., "file-created")
     */
    type: string;
    /**
     * The language identifier for which this action is triggered
     */
    languageId: string;
    /**
     * Additional data specific to the trigger type
     */
    data: unknown;
}

/**
 * Response with a dialog page to show.
 */
export interface ActionStartPageResponse {
    /**
     * Discriminator for the response type
     */
    kind: "page";
    /**
     * The first page of the action dialog
     */
    page: ActionDialogPage;
}

/**
 * Response indicating the action completed immediately without needing a dialog.
 */
export interface ActionStartCompletionResponse {
    /**
     * Discriminator for the response type
     */
    kind: "completion";
    /**
     * Optional list of executions to create after completion.
     * Each execution contains the file path and data needed to start an execution.
     */
    executions?: ActionExecutionRequest[];
}

/**
 * Response returned when starting an action dialog.
 * Can either show a dialog page or complete immediately.
 */
export type ActionStartResponse = ActionStartPageResponse | ActionStartCompletionResponse;

/**
 * Parameters for submitting an action dialog page.
 */
export interface ActionSubmitParams {
    /**
     * The original parameters used to start the action dialog
     */
    config: ActionStartParams;
    /**
     * All input values collected from the current and all previous pages.
     * Keys are JSON paths to the form fields.
     */
    inputs: Record<string, unknown>[];
}

/**
 * Represents a single validation error for an action dialog submission.
 */
export interface ActionValidationError {
    /**
     * JSON path to the field that has the error (e.g., "/name" or "/items/0/value")
     */
    path: string;
    /**
     * Human-readable error message describing the validation failure
     */
    message: string;
}

/**
 * Response indicating validation errors in the submission.
 */
export interface ActionSubmitValidationResponse {
    /**
     * Discriminator for the response type
     */
    kind: "validation";
    /**
     * List of validation errors found in the submission
     */
    errors: ActionValidationError[];
}

/**
 * Response indicating the next page should be displayed.
 */
export interface ActionSubmitNextPageResponse {
    /**
     * Discriminator for the response type
     */
    kind: "nextPage";
    /**
     * The next page to display in the dialog
     */
    page: ActionDialogPage;
}

/**
 * Response indicating the action dialog has completed successfully.
 */
export interface ActionSubmitCompletionResponse {
    /**
     * Discriminator for the response type
     */
    kind: "completion";
    /**
     * Optional list of executions to create after completion.
     * Each execution contains the file path and data needed to start an execution.
     */
    executions?: ActionExecutionRequest[];
}

/**
 * Request data for creating an execution from an action.
 */
export interface ActionExecutionRequest {
    /**
     * Path to the file to execute
     */
    filePath: string;
    /**
     * Arbitrary JSON data for the execution (e.g., function to execute)
     */
    data: unknown;
}

/**
 * Response returned when submitting an action dialog page.
 * Can be one of:
 * - Validation errors if the submission is invalid
 * - Next page if there are more pages to display
 * - Completion if this was the last page and submission succeeded
 */
export type ActionSubmitResponse =
    | ActionSubmitValidationResponse
    | ActionSubmitNextPageResponse
    | ActionSubmitCompletionResponse;

/**
 * Location where an action can be displayed.
 */
export enum ActionDisplayLocation {
    /**
     * Action displayed in the context menu for a file
     */
    CONTEXT_MENU = "contextMenu",
    /**
     * Action displayed in the editor title area
     */
    EDITOR_TITLE = "editorTitle"
}

/**
 * Represents an action that can be triggered for a file.
 */
export interface FileAction {
    /**
     * Display name of the action
     */
    name: string;
    /**
     * Icon for the action (serializable Lucide IconNode)
     */
    icon: ActionIconNode;
    /**
     * The key/identifier of the action it triggers
     */
    key: string;
    /**
     * Where this action should be displayed
     */
    displayLocations: ActionDisplayLocation[];
}

/**
 * Parameters for getting actions available for a file.
 */
export interface GetFileActionsParams {
    /**
     * The language identifier of the file
     */
    languageId: string;
    /**
     * The URI of the file
     */
    fileUri: string;
}

/**
 * Response containing the list of actions available for a file.
 */
export interface GetFileActionsResponse {
    /**
     * List of actions available for the file
     */
    actions: FileAction[];
}

/**
 * Creates a namespace containing JSON-RPC message types for action dialog communication.
 * Defines the protocol methods used for action dialog interactions.
 *
 * @param vscodeJsonrpc The vscode-jsonrpc module from the plugin context
 * @returns A namespace object with action protocol definitions
 */
export function createActionProtocol(vscodeJsonrpc: PluginContext["vscode-jsonrpc"]) {
    const { RequestType } = vscodeJsonrpc;

    return {
        /**
         * Request type for starting an action dialog.
         * Used to initiate an action dialog flow.
         */
        ActionStartRequest: new RequestType<ActionStartParams, ActionStartResponse, void>("action/start"),

        /**
         * Request type for submitting an action dialog page.
         * Used to submit form data for the current dialog page.
         */
        ActionSubmitRequest: new RequestType<ActionSubmitParams, ActionSubmitResponse, void>("action/submit"),

        /**
         * Request type for getting available actions for a file.
         * Used to populate context menus and editor title actions.
         */
        GetFileActionsRequest: new RequestType<GetFileActionsParams, GetFileActionsResponse, void>(
            "action/getFileActions"
        )
    };
}
