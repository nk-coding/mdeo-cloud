import type {
    ActionDialogPage,
    ActionSchema,
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    NewFileActionData
} from "@mdeo/language-common";
import type { ActionHandler } from "@mdeo/language-shared";
import type { LangiumSharedServices } from "langium/lsp";
import { URI } from "langium";

/**
 * Action service for handling new file creation in the Model language.
 *
 * This action:
 *  - Collects all metamodel (.mm) files in the workspace
 *  - Presents them as *relative paths* to the new file
 *  - Inserts a `using "<relative-path>"` statement at the top of the file
 */
export class ModelActionHandler implements ActionHandler {
    /**
     * Shared Langium services provided by the language server.
     */
    private sharedServices: LangiumSharedServices;

    /**
     * URI string of the newly created model file.
     * Used as the base for computing relative paths.
     */
    private newFilePath?: string;

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
        if (params.type !== "new-file") {
            throw new Error(`Unsupported action type: ${params.type}`);
        }

        const data = params.data as NewFileActionData;
        this.newFilePath = data.filePath;

        const metamodelFiles = await this.findMetamodelFiles();

        const options: string[] = [];
        const basePath = URI.parse(this.newFilePath).path;

        for (const absolutePath of metamodelFiles) {
            options.push(this.calculateRelativePath(basePath, absolutePath));
        }

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
            description: "Choose the metamodel to use for this model file",
            schema,
            isLastPage: true,
            submitButtonLabel: "Insert Using Statement"
        };

        return { page };
    }

    /**
     * Submits the dialog and inserts the `using` statement.
     *
     * The selected value is already a relative path and is inserted verbatim.
     *
     * @param params The submit parameters with selected metamodel
     * @returns Completion response after inserting the statement
     */
    async submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        const inputs = params.inputs[0] || {};
        const metamodelPath = inputs["metamodel"] as string;

        if (!metamodelPath) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodel", message: "Please select a metamodel file" }]
            };
        }

        await this.insertUsingStatement(metamodelPath);
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
     * Inserts a `using` statement at the beginning of the new file.
     *
     * @param relativePath Relative path to the selected metamodel
     */
    private async insertUsingStatement(relativePath: string): Promise<void> {
        if (!this.newFilePath) {
            return;
        }

        const usingStatement = `using "${relativePath}"\n\n`;

        const connection = this.sharedServices.lsp.Connection!;

        const workspaceEdit = {
            changes: {
                [this.newFilePath]: [
                    {
                        range: {
                            start: { line: 0, character: 0 },
                            end: { line: 0, character: 0 }
                        },
                        newText: usingStatement
                    }
                ]
            }
        };

        await connection.workspace.applyEdit({ edit: workspaceEdit });
    }

    /**
     * Calculates the relative path from one file to another.
     *
     * @param fromPath Absolute path of the source file
     * @param toPath Absolute path of the target file
     * @returns Relative path from source file to target file
     */
    private calculateRelativePath(fromPath: string, toPath: string): string {
        const fromParts = fromPath.split("/");
        const toParts = toPath.split("/");

        fromParts.pop();

        let commonLength = 0;
        while (
            commonLength < fromParts.length &&
            commonLength < toParts.length &&
            fromParts[commonLength] === toParts[commonLength]
        ) {
            commonLength++;
        }

        const upLevels = fromParts.length - commonLength;
        const relativeParts: string[] = ["."];

        for (let i = 0; i < upLevels; i++) {
            relativeParts.push("..");
        }

        relativeParts.push(...toParts.slice(commonLength));
        return relativeParts.join("/");
    }
}
