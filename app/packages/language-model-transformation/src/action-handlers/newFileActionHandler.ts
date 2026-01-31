import type {
    ActionDialogPage,
    ActionSchema,
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    NewFileActionData
} from "@mdeo/language-common";
import { calculateRelativePath, type ActionHandler } from "@mdeo/language-shared";
import type { LangiumSharedServices } from "langium/lsp";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { URI } from "langium";

/**
 * Input data collected from the dialog.
 */
interface MetamodelSelectionInputs {
    metamodel: string;
}

/**
 * Type guard to check if a value is MetamodelSelectionInputs.
 */
function isMetamodelSelectionInputs(value: unknown): value is MetamodelSelectionInputs {
    return typeof value === "object" && value !== null && "metamodel" in value;
}

/**
 * Handler for the "new-file" action in the Model Transformation language.
 *
 * This handler:
 *  - Collects all metamodel (.mm) files in the workspace
 *  - Presents them as relative paths to the new file
 *  - Inserts an `import "<relative-path>"` statement at the top of the file
 */
export class NewFileActionHandler implements ActionHandler {
    /**
     * Shared Langium services for accessing workspace documents.
     */
    private readonly sharedServices: LangiumSharedServices;

    /**
     * Creates a new file action handler.
     *
     * @param sharedServices Shared Langium services
     */
    constructor(sharedServices: LangiumSharedServices) {
        this.sharedServices = sharedServices;
    }

    /**
     * Starts the new file action dialog.
     *
     * This method:
     *  - Stores the path of the newly created file
     *  - Finds all metamodel (.mm) files in the workspace
     *  - Computes their relative paths to the new file
     *  - Presents those paths in a combobox
     *
     * @param params The action start parameters with file path data
     * @returns The dialog page with metamodel selection
     */
    async startAction(params: ActionStartParams): Promise<ActionStartResponse> {
        const data = params.data as NewFileActionData;

        const metamodelFiles = await this.findMetamodelFiles();
        const options = this.buildFileOptions(data.uri, metamodelFiles);

        return this.createSelectionPage(options);
    }

    /**
     * Submits the dialog and returns the workspace edit for inserting the `import` statement.
     *
     * @param params The submit parameters with selected metamodel
     * @returns Completion response with workspace edit for inserting the statement
     */
    async submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        const data = params.config.data as NewFileActionData;
        const rawInputs = params.inputs[0];

        if (!isMetamodelSelectionInputs(rawInputs)) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodel", message: "Invalid input structure" }]
            };
        }

        const metamodelPath = rawInputs.metamodel;

        if (!metamodelPath) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodel", message: "Please select a metamodel file" }]
            };
        }

        const workspaceEdit = this.createUsingStatementEdit(data.uri, metamodelPath);
        const connection = this.sharedServices.lsp.Connection;
        await connection?.workspace.applyEdit(workspaceEdit);

        return { kind: "completion" };
    }

    /**
     * Finds all metamodel (.mm) files in the workspace.
     *
     * @returns Array of absolute workspace paths to metamodel files
     */
    private async findMetamodelFiles(): Promise<string[]> {
        const documents = this.sharedServices.workspace.LangiumDocuments.all.toArray();
        return documents.filter((doc) => doc.textDocument.languageId === "metamodel").map((doc) => doc.uri.path);
    }

    /**
     * Builds the list of file options as relative paths.
     *
     * @param uri The URI of the new file
     * @param metamodelFiles The list of metamodel file paths
     * @returns Array of relative paths for the selection dropdown
     */
    private buildFileOptions(uri: string, metamodelFiles: string[]): string[] {
        const basePath = URI.parse(uri).path;
        return metamodelFiles.map((absolutePath) => calculateRelativePath(basePath, absolutePath));
    }

    /**
     * Creates the metamodel selection dialog page.
     *
     * @param options The list of file options
     * @returns The dialog page response
     */
    private createSelectionPage(options: string[]): ActionStartResponse {
        const schema: ActionSchema = {
            properties: {
                metamodel: {
                    enum: options,
                    combobox: true,
                    placeholder: "Select or enter a metamodel file path"
                }
            },
            propertyLabels: {
                metamodel: "Metamodel File"
            }
        };

        const page: ActionDialogPage = {
            title: "Select Metamodel",
            description: "Choose the metamodel to use for this transformation file",
            schema,
            isLastPage: true,
            submitButtonLabel: "Insert Using Statement"
        };

        return { kind: "page", page };
    }

    /**
     * Creates a workspace edit for inserting a `using` statement at the beginning of the file.
     *
     * @param uri The URI of the file
     * @param relativePath Relative path to the selected metamodel
     * @returns The workspace edit for inserting the using statement
     */
    private createUsingStatementEdit(uri: string, relativePath: string): WorkspaceEdit {
        const importStatement = `using "${relativePath}"\n\n`;

        return {
            changes: {
                [uri]: [
                    {
                        range: {
                            start: { line: 0, character: 0 },
                            end: { line: 0, character: 0 }
                        },
                        newText: importStatement
                    }
                ]
            }
        };
    }
}
