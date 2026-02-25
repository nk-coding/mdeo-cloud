import {
    type ActionStartParams,
    type ActionStartResponse,
    type ActionSubmitParams,
    type ActionSubmitResponse,
    type ActionSchema,
    type ActionExecutionRequest,
    type FileMenuActionData,
    parseUri,
    FileCategory
} from "@mdeo/language-common";
import { sharedImport, type ActionHandler, resolveRelativePath, calculateRelativePath } from "@mdeo/language-shared";
import { isMetamodelCompatible } from "@mdeo/language-metamodel";
import type { LangiumSharedServices } from "langium/lsp";
import { type ScriptType } from "../grammar/scriptTypes.js";
import type { ModelType } from "@mdeo/language-model";

const { URI, UriUtils } = sharedImport("langium");

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
 * Input data collected when both method and model selection are shown.
 */
interface MethodAndModelSelectionInputs {
    /**
     * The name of the selected method to execute.
     */
    methodName: string;
    /**
     * Optional relative path to the selected model file.
     */
    modelPath?: string;
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
        const data = params.data as FileMenuActionData;

        const methods = await this.findParameterlessMethods(data.uri);

        if (methods.length === 0) {
            return {
                kind: "page",
                page: {
                    title: "Run Script Method",
                    description:
                        "No parameterless methods found in this file. Only methods with no parameters can be executed.",
                    schema: {
                        properties: {}
                    },
                    isLastPage: true
                }
            };
        }

        const metamodelUri = await this.getScriptMetamodelUri(data.uri);
        if (metamodelUri) {
            const compatibleModels = await this.findCompatibleModels(data.uri, metamodelUri);
            return this.createCombinedSelectionPage(methods, compatibleModels);
        }

        if (methods.length === 1) {
            return this.createExecutionStart(data.uri, methods[0]);
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
        const data = params.config.data as FileMenuActionData;
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

        const modelPath = (rawInputs as MethodAndModelSelectionInputs).modelPath;

        return this.createExecutionSubmit(data.uri, methodName, modelPath);
    }

    /**
     * Finds all parameterless methods in the script file.
     *
     * @returns Array of method names that have no parameters
     */
    private async findParameterlessMethods(uri: string): Promise<string[]> {
        const parsedUri = URI.parse(uri);
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(parsedUri);

        if (!document || document.parseResult.lexerErrors.length > 0 || document.parseResult.parserErrors.length > 0) {
            return [];
        }

        const root = document.parseResult.value as ScriptType;

        const parameterlessFunctions = root.functions.filter((func) => {
            return func.parameterList == undefined || func.parameterList.parameters.length === 0;
        });

        return parameterlessFunctions.map((func) => func.name);
    }

    /**
     * Gets the metamodel URI referenced by a script file.
     *
     * @param uri URI of the script file
     * @returns Absolute URI of the metamodel, or undefined if not found
     */
    private async getScriptMetamodelUri(uri: string): Promise<string | undefined> {
        const parsedUri = URI.parse(uri);
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(parsedUri);

        if (!document || document.parseResult.lexerErrors.length > 0 || document.parseResult.parserErrors.length > 0) {
            return undefined;
        }

        const root = document.parseResult.value as ScriptType;
        const metamodelImport = root.metamodelImport?.file;

        if (!metamodelImport) {
            return undefined;
        }

        return resolveRelativePath(document, metamodelImport).toString();
    }

    /**
     * Gets the metamodel URI from a model file.
     *
     * @param modelUri URI of the model file
     * @returns Absolute URI of the metamodel, or undefined if not found
     */
    private getModelMetamodelUri(modelUri: string): string | undefined {
        const parsedUri = URI.parse(modelUri);
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(parsedUri);

        if (!document || document.parseResult.lexerErrors.length > 0 || document.parseResult.parserErrors.length > 0) {
            return undefined;
        }

        const root = document.parseResult.value as ModelType;
        const metamodelImport = root.import?.file;

        if (!metamodelImport) {
            return undefined;
        }

        return resolveRelativePath(document, metamodelImport).toString();
    }

    /**
     * Finds all model files that reference the same metamodel as the script.
     *
     * @param scriptUri URI of the script file
     * @param metamodelUri Absolute URI of the metamodel
     * @returns Array of relative paths to compatible model files
     */
    private async findCompatibleModels(scriptUri: string, metamodelUri: string): Promise<string[]> {
        const langiumDocuments = this.sharedServices.workspace.LangiumDocuments;
        const documents = langiumDocuments.all.toArray();
        const modelDocuments = documents.filter((doc) => doc.textDocument.languageId === "model");
        const compatibleModels: string[] = [];
        const scriptPath = URI.parse(scriptUri).path;

        const metamodelDoc = langiumDocuments.getDocument(URI.parse(metamodelUri));
        if (!metamodelDoc) {
            return compatibleModels;
        }

        for (const doc of modelDocuments) {
            const modelMetamodelUri = this.getModelMetamodelUri(doc.uri.toString());
            if (!modelMetamodelUri) continue;

            const modelMetamodelDoc = langiumDocuments.getDocument(URI.parse(modelMetamodelUri));
            if (!modelMetamodelDoc) continue;

            if (isMetamodelCompatible(modelMetamodelDoc, metamodelDoc, langiumDocuments)) {
                const relativePath = calculateRelativePath(scriptPath, doc.uri.path);
                compatibleModels.push(relativePath);
            }
        }

        return compatibleModels;
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
     * Creates a combined method + model selection dialog page.
     *
     * @param methods The list of available parameterless methods
     * @param models The list of compatible model file paths
     * @returns The dialog page response
     */
    private createCombinedSelectionPage(methods: string[], models: string[]): ActionStartResponse {
        const schema: ActionSchema = {
            properties: {
                methodName: {
                    enum: methods,
                    placeholder: "Select a method to execute"
                },
                modelPath: {
                    enum: models,
                    placeholder: models.length > 0 ? "Select a model file" : "No compatible models found"
                }
            },
            propertyLabels: {
                methodName: "Method",
                modelPath: "Model File"
            }
        };

        return {
            kind: "page",
            page: {
                title: "Run Script Method",
                description: "Select a method and model file to execute.",
                schema,
                isLastPage: true,
                submitButtonLabel: "Run"
            }
        };
    }

    /**
     * Creates an execution request for the given method name.
     *
     * @param uri The URI of the script file
     * @param methodName The name of the method to execute
     * @param modelPath Optional relative path to the model file
     * @returns The execution request
     */
    private createExecutionRequest(uri: string, methodName: string, modelPath?: string): ActionExecutionRequest {
        const parsedUri = parseUri(URI.parse(uri));
        if (parsedUri.category !== FileCategory.RegularFile) {
            throw new Error("Invalid file category for script execution");
        }
        const filePath = parsedUri.path;

        const absoluteModelPath = modelPath ? this.resolveModelPath(uri, modelPath) : undefined;

        return {
            filePath,
            data: {
                filePath,
                methodName,
                ...(absoluteModelPath !== undefined ? { modelPath: absoluteModelPath } : {})
            }
        };
    }

    /**
     * Resolves a relative model path to an absolute path.
     *
     * @param scriptUri URI of the script file
     * @param relativeModelPath Relative path to the model file
     * @returns Absolute path to the model file
     */
    private resolveModelPath(scriptUri: string, relativeModelPath: string): string {
        const scriptParsedUri = URI.parse(scriptUri);
        const dirname = UriUtils.dirname(scriptParsedUri);
        const absoluteUri = UriUtils.joinPath(dirname, relativeModelPath);

        const parsedUri = parseUri(absoluteUri);
        if (parsedUri.category !== FileCategory.RegularFile) {
            throw new Error("Invalid model file path");
        }

        return parsedUri.path;
    }

    /**
     * Creates an execution completion response for startAction.
     *
     * @param uri The URI of the script file
     * @param methodName The name of the method to execute
     * @returns The start completion response with execution request
     */
    private createExecutionStart(uri: string, methodName: string): ActionStartResponse {
        const executionRequest = this.createExecutionRequest(uri, methodName);

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
     * @param uri The URI of the script file
     * @param methodName The name of the method to execute
     * @param modelPath Optional relative path to the model file
     * @returns The submit completion response with execution request
     */
    private createExecutionSubmit(uri: string, methodName: string, modelPath?: string): ActionSubmitResponse {
        const executionRequest = this.createExecutionRequest(uri, methodName, modelPath);

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
