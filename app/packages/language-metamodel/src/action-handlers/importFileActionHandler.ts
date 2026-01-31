import type {
    ActionDialogPage,
    ActionSchema,
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    FileMenuActionData,
    WorkspaceEditService,
    WorkspaceEditAdditionalServices,
    AstReflection,
    ExtendedLangiumServices
} from "@mdeo/language-common";
import { calculateRelativePath, sharedImport, type ActionHandler } from "@mdeo/language-shared";
import type { LangiumDocument } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { FileImport, type MetaModelType, type FileImportType } from "../grammar/metamodelTypes.js";
import type { LangiumLSPServices, LangiumSharedServices } from "langium/lsp";

const { URI } = sharedImport("langium");

/**
 * Input data collected from the file selection dialog.
 */
interface FileSelectionInputs {
    metamodelFile: string;
}

/**
 * Type guard to check if a value is FileSelectionInputs.
 *
 * @param value The value to check
 * @returns True if the value is FileSelectionInputs
 */
function isFileSelectionInputs(value: unknown): value is FileSelectionInputs {
    return (
        typeof value === "object" &&
        value !== null &&
        "metamodelFile" in value &&
        typeof (value as FileSelectionInputs).metamodelFile === "string"
    );
}

/**
 * Action handler for importing an entire metamodel file.
 *
 * This handler provides a single-step dialog to select the file to import.
 * The import statement format is: import "./relativePath"
 */
export class ImportFileActionHandler implements ActionHandler {
    /**
     * Shared Langium services for accessing workspace documents.
     */
    private readonly sharedServices: LangiumSharedServices;

    /**
     * Workspace edit service for creating edits.
     */
    private readonly workspaceEditService: WorkspaceEditService;

    /**
     * AST reflection for type checking.
     */
    private readonly reflection: AstReflection;

    /**
     * Creates an import file action handler.
     *
     * @param services Langium services with workspace edit support
     */
    constructor(
        private readonly services: LangiumLSPServices & WorkspaceEditAdditionalServices & ExtendedLangiumServices
    ) {
        this.sharedServices = services.shared;
        this.workspaceEditService = services.workspace.WorkspaceEdit;
        this.reflection = services.shared.AstReflection;
    }

    /**
     * Starts the import action dialog.
     * Shows a file selection dialog with available metamodel files.
     *
     * @param params The action start parameters with file path data
     * @returns The dialog page with file selection
     */
    async startAction(params: ActionStartParams): Promise<ActionStartResponse> {
        const data = params.data as FileMenuActionData;
        const currentFilePath = URI.parse(data.uri).path;
        const metamodelFiles = await this.findMetamodelFiles(currentFilePath);
        const options = this.buildFileOptions(currentFilePath, metamodelFiles);

        return this.createFileSelectionPage(options);
    }

    /**
     * Submits form data and returns completion with workspace edit.
     * Validates the selection and creates the import statement.
     *
     * @param params The submit parameters with collected inputs
     * @returns Validation errors or completion with workspace edit
     */
    async submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        const data = params.config.data as FileMenuActionData;
        const rawInputs = params.inputs[0];

        if (!isFileSelectionInputs(rawInputs)) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodelFile", message: "Invalid input structure" }]
            };
        }

        const selectedFile = rawInputs.metamodelFile;

        if (!selectedFile) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodelFile", message: "Please select a metamodel file" }]
            };
        }

        const conflict = this.checkDuplicateImport(data.uri, selectedFile);
        if (conflict !== null) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodelFile", message: conflict }]
            };
        }

        const workspaceEdit = await this.createImportEdit(data.uri, selectedFile);
        const connection = this.sharedServices.lsp.Connection;
        await connection?.workspace.applyEdit(workspaceEdit);

        return { kind: "completion" };
    }

    /**
     * Checks if the file is already imported.
     *
     * @param currentFileUri The URI of the current file
     * @param relativePath The relative path of the file to import
     * @returns An error message if there's a conflict, null otherwise
     */
    private checkDuplicateImport(currentFileUri: string, relativePath: string): string | null {
        const document = this.findCurrentDocument(currentFileUri);
        if (document == undefined) {
            return null;
        }

        const root = document.parseResult.value as MetaModelType;
        if (root.imports == undefined) {
            return null;
        }

        const existingImport = root.imports.find(
            (imp) => this.reflection.isInstance(imp, FileImport) && imp.file === relativePath
        );

        if (existingImport !== undefined) {
            return `The file "${relativePath}" is already imported`;
        }

        return null;
    }

    /**
     * Creates the workspace edit for the import statement.
     *
     * @param currentFileUri The URI of the current file
     * @param relativePath The relative path to import
     * @returns The workspace edit
     */
    private async createImportEdit(currentFileUri: string, relativePath: string): Promise<WorkspaceEdit> {
        const document = this.findCurrentDocument(currentFileUri);

        if (document == undefined) {
            throw new Error(`Document not found: ${currentFileUri}`);
        }

        const root = document.parseResult.value as MetaModelType;
        const importStatement = await this.buildImportStatement(relativePath, document);

        if (root.imports != undefined && root.imports.length > 0) {
            const fileImports = root.imports.filter((imp) => this.reflection.isInstance(imp, FileImport));
            if (fileImports.length > 0) {
                return this.insertAfterLastImport(fileImports, importStatement, document);
            }
        }

        return this.insertAtBeginning(importStatement, currentFileUri);
    }

    /**
     * Builds an import statement string.
     *
     * @param relativePath The relative path to import
     * @param document The document being edited
     * @returns The formatted import statement
     */
    private async buildImportStatement(relativePath: string, document: LangiumDocument): Promise<string> {
        const importNode: FileImportType = {
            $type: FileImport.name,
            file: relativePath
        };
        return this.workspaceEditService.serializeNode(importNode, document);
    }

    /**
     * Inserts an import statement after the last existing import.
     *
     * @param imports The existing imports
     * @param importStatement The import statement to insert
     * @param document The document being edited
     * @returns The workspace edit
     */
    private async insertAfterLastImport(
        imports: FileImportType[],
        importStatement: string,
        document: LangiumDocument
    ): Promise<WorkspaceEdit> {
        const lastImport = imports[imports.length - 1];
        const cstNode = lastImport.$cstNode;

        if (cstNode == undefined) {
            throw new Error("CST node not found for existing import");
        }

        return this.workspaceEditService.createInsertAfterNodeEdit(cstNode, importStatement, document, true);
    }

    /**
     * Inserts an import statement at the beginning of the file.
     *
     * @param importStatement The import statement to insert
     * @param currentFileUri The URI of the current file
     * @returns The workspace edit
     */
    private insertAtBeginning(importStatement: string, currentFileUri: string): WorkspaceEdit {
        return {
            changes: {
                [currentFileUri]: [
                    {
                        range: {
                            start: { line: 0, character: 0 },
                            end: { line: 0, character: 0 }
                        },
                        newText: importStatement + "\n\n"
                    }
                ]
            }
        };
    }

    /**
     * Finds the current document from its URI.
     *
     * @param currentFileUri The URI to find
     * @returns The document or undefined
     */
    private findCurrentDocument(currentFileUri: string): LangiumDocument | undefined {
        return this.sharedServices.workspace.LangiumDocuments.all
            .toArray()
            .find((doc: LangiumDocument) => doc.uri.toString() === currentFileUri);
    }

    /**
     * Finds all metamodel files in the workspace, excluding the current file.
     *
     * @param currentFilePath The path of the current file to exclude
     * @returns Array of absolute paths to metamodel files
     */
    private async findMetamodelFiles(currentFilePath: string): Promise<string[]> {
        const documents = this.sharedServices.workspace.LangiumDocuments.all.toArray();
        return documents
            .filter(
                (doc: LangiumDocument) =>
                    doc.textDocument.languageId === "metamodel" && doc.uri.path !== currentFilePath
            )
            .map((doc: LangiumDocument) => doc.uri.path);
    }

    /**
     * Builds the list of file options as relative paths.
     *
     * @param currentFilePath The path of the current file
     * @param metamodelFiles The list of metamodel file paths
     * @returns Array of relative paths for the file selection dropdown
     */
    private buildFileOptions(currentFilePath: string, metamodelFiles: string[]): string[] {
        return metamodelFiles.map((absolutePath) => calculateRelativePath(currentFilePath, absolutePath));
    }

    /**
     * Creates the file selection dialog page.
     *
     * @param options The list of file options
     * @returns The dialog page response
     */
    private createFileSelectionPage(options: string[]): ActionStartResponse {
        const schema: ActionSchema = {
            properties: {
                metamodelFile: {
                    enum: options,
                    combobox: true,
                    placeholder: "Select a metamodel file"
                }
            },
            propertyLabels: {
                metamodelFile: "Metamodel File"
            }
        };

        const page: ActionDialogPage = {
            title: "Import File",
            description: "Select the metamodel file to import",
            schema,
            isLastPage: true,
            submitButtonLabel: "Import"
        };

        return { kind: "page", page };
    }
}
