import type {
    ActionDialogPage,
    ActionSchema,
    ActionSchemaFileSelectNode,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    NewFileActionData
} from "@mdeo/language-common";
import { buildFileSelectTree, calculateRelativePath, sharedImport, type ActionHandler } from "@mdeo/language-shared";
import type { LangiumSharedServices } from "langium/lsp";
import type { WorkspaceEdit } from "vscode-languageserver-types";

const { URI } = sharedImport("langium");

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
 * Handler for the "new-file" action in the Script language.
 *
 * This handler:
 *  - Collects all metamodel (.mm) files in the workspace
 *  - Presents them as relative paths to the new file
 *  - Optionally inserts a `using "<relative-path>"` statement at the top of the file
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
     * @returns The dialog page with metamodel selection
     */
    async startAction(): Promise<ActionStartResponse> {
        const metamodelFiles = await this.findMetamodelFiles();
        const { nodes, rootPath } = buildFileSelectTree(metamodelFiles);

        return this.createSelectionPage(nodes, rootPath);
    }

    /**
     * Submits the dialog and optionally inserts a `using` statement.
     *
     * If no metamodel is selected, the action completes without applying edits.
     *
     * @param params The submit parameters with selected metamodel
     * @returns Completion response with optional workspace edit
     */
    async submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        const data = params.config.data as NewFileActionData;
        const rawInputs = params.inputs[0];

        if (!isMetamodelSelectionInputs(rawInputs)) {
            return { kind: "completion" };
        }

        const metamodelAbsolutePath = rawInputs.metamodel;

        if (!metamodelAbsolutePath) {
            return { kind: "completion" };
        }

        const fromPath = URI.parse(data.uri).path;
        const relativePath = calculateRelativePath(fromPath, metamodelAbsolutePath);
        const workspaceEdit = this.createUsingStatementEdit(data.uri, relativePath);
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
     * Creates the metamodel selection dialog page.
     *
     * @param tree The file tree nodes
     * @param basePath The absolute path of the new file
     * @returns The dialog page response
     */
    private createSelectionPage(tree: ActionSchemaFileSelectNode[], rootPath: string): ActionStartResponse {
        const schema: ActionSchema = {
            properties: {
                metamodel: {
                    fileSelect: tree,
                    rootPath,
                    placeholder: "Select a metamodel file"
                }
            },
            propertyLabels: {
                metamodel: "Metamodel File"
            }
        };

        const page: ActionDialogPage = {
            title: "Select Metamodel",
            description:
                "Choose an optional metamodel to use for this script file (you can submit without selecting one)",
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
        const usingStatement = `using "${relativePath}"\n\n`;

        return {
            changes: {
                [uri]: [
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
    }
}
