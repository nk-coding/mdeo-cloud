import type {
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    ActionSchema,
    ActionExecutionRequest
} from "@mdeo/language-common";
import type { ActionHandler } from "@mdeo/language-shared";
import type { LangiumSharedServices } from "langium/lsp";
import { URI } from "langium";
import { Script, type FunctionType, type ScriptType } from "../grammar/scriptTypes.js";

/**
 * Input data collected from the dialog.
 */
interface MethodSelectionInputs {
    /**
     * The name of the selected method to execute.
     */
    methodName: string;
}

/**
 * Type guard to check if a value is MethodSelectionInputs.
 *
 * @param value The value to check
 * @returns True if the value is MethodSelectionInputs
 */
function isMethodSelectionInputs(value: unknown): value is MethodSelectionInputs {
    return typeof value === "object" && value !== null && "methodName" in value;
}

/**
 * Handler for the "run" action in the Script language.
 *
 * This handler:
 *  - Parses the script file to extract all functions
 *  - Filters to only show parameterless functions (since execution only supports those)
 *  - Presents them as a dropdown selection
 *  - When selected, creates an execution request with the method name
 */
export class RunScriptActionHandler implements ActionHandler {
    /**
     * Shared Langium services for accessing workspace documents.
     */
    private readonly sharedServices: LangiumSharedServices;

    /**
     * URI string of the script file to execute.
     */
    private fileUri?: string;

    /**
     * Creates a new run script action handler.
     *
     * @param sharedServices Shared Langium services
     */
    constructor(sharedServices: LangiumSharedServices) {
        this.sharedServices = sharedServices;
    }

    /**
     * Starts the run action dialog.
     *
     * This method:
     *  - Stores the file URI
     *  - Parses the script file to find all functions
     *  - Filters to only parameterless functions
     *  - Presents those functions in a dropdown
     *
     * @param params The action start parameters with file URI data
     * @returns The dialog page with method selection or immediate completion
     */
    async startAction(params: ActionStartParams): Promise<ActionStartResponse> {
        const data = params.data as { uri: string };
        this.fileUri = data.uri;

        const methods = await this.findParameterlessMethods();

        if (methods.length === 0) {
            // Return a page with an error message
            return {
                kind: "page",
                page: {
                    title: "Run Script Method",
                    description: "No parameterless methods found in this file. Only methods with no parameters can be executed.",
                    schema: {
                        properties: {}
                    },
                    isLastPage: true
                }
            };
        }

        // If only one method, execute it directly
        if (methods.length === 1) {
            return this.createExecutionStart(methods[0]);
        }

        return this.createSelectionPage(methods);
    }

    /**
     * Submits the dialog and returns the execution request.
     *
     * @param params The submit parameters with selected method
     * @returns Completion response with execution request
     */
    async submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        const rawInputs = params.inputs[0];

        if (!isMethodSelectionInputs(rawInputs)) {
            return {
                kind: "validation",
                errors: [{ path: "/methodName", message: "Invalid input structure" }]
            };
        }

        const methodName = rawInputs.methodName;

        if (!methodName) {
            return {
                kind: "validation",
                errors: [{ path: "/methodName", message: "Please select a method to execute" }]
            };
        }

        return this.createExecutionSubmit(methodName);
    }

    /**
     * Finds all parameterless methods in the script file.
     *
     * @returns Array of method names that have no parameters
     */
    private async findParameterlessMethods(): Promise<string[]> {
        if (!this.fileUri) {
            return [];
        }

        const uri = URI.parse(this.fileUri);
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(uri);

        if (!document || document.parseResult.lexerErrors.length > 0 || document.parseResult.parserErrors.length > 0) {
            return [];
        }

        const root = document.parseResult.value;
        if (root.$type !== Script.$type) {
            return [];
        }

        const scriptRoot = root as ScriptType;

        // Filter to only functions with no parameters
        const parameterlessFunctions = scriptRoot.functions.filter((func) => {
            return func.parameterList !== undefined && func.parameterList.parameters.length === 0;
        });

        return parameterlessFunctions.map((func) => func.name);
    }

    /**
     * Creates the method selection dialog page.
     *
     * @param methods The list of available methods
     * @returns The dialog page response
     */
    private createSelectionPage(methods: string[]): ActionStartResponse {
        const schema: ActionSchema = {
            properties: {
                methodName: {
                    enum: methods,
                    placeholder: "Select a method to execute"
                }
            },
            propertyLabels: {
                methodName: "Method"
            }
        };

        return {
            kind: "page",
            page: {
                title: "Run Script Method",
                description: "Select a method to execute. Only methods with no parameters are shown.",
                schema,
                isLastPage: true,
                submitButtonLabel: "Run"
            }
        };
    }

    /**
     * Creates an execution request for the given method name.
     *
     * @param methodName The name of the method to execute
     * @returns The execution request or null if file URI is not available
     */
    private createExecutionRequest(methodName: string): ActionExecutionRequest | null {
        if (!this.fileUri) {
            return null;
        }

        const uri = URI.parse(this.fileUri);
        const filePath = uri.path;

        return {
            filePath,
            data: {
                filePath,
                methodName
            }
        };
    }

    /**
     * Creates an execution completion response for startAction.
     *
     * @param methodName The name of the method to execute
     * @returns The start completion response with execution request
     */
    private createExecutionStart(methodName: string): ActionStartResponse {
        const executionRequest = this.createExecutionRequest(methodName);
        
        if (!executionRequest) {
            return {
                kind: "page",
                page: {
                    title: "Run Script Method",
                    description: "File URI not available",
                    schema: {
                        properties: {}
                    },
                    isLastPage: true
                }
            };
        }

        return {
            kind: "completion",
            executions: [executionRequest]
        };
    }

    /**
     * Creates an execution completion response for submitAction.
     *
     * @param methodName The name of the method to execute
     * @returns The submit completion response with execution request
     */
    private createExecutionSubmit(methodName: string): ActionSubmitResponse {
        const executionRequest = this.createExecutionRequest(methodName);
        
        if (!executionRequest) {
            return {
                kind: "validation",
                errors: [{ path: "/", message: "File URI not available" }]
            };
        }

        return {
            kind: "completion",
            executions: [executionRequest]
        };
    }
}
