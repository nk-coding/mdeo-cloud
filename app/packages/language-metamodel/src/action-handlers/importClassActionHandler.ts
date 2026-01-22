import type {
    ActionDialogPage,
    ActionSchema,
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    FileMenuActionData,
    WorkspaceEditService,
    WorkspaceEditAdditionalServices
} from "@mdeo/language-common";
import { calculateRelativePath, type ActionHandler } from "@mdeo/language-shared";
import type { LangiumDocument } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { URI } from "langium";
import {
    ClassFileImport,
    type ClassImportType,
    type ClassType,
    type MetaModelType
} from "../grammar/metamodelTypes.js";
import type { LangiumLSPServices, LangiumSharedServices } from "langium/lsp";
import type { ASTType } from "@mdeo/language-common";

/**
 * Type for ClassFileImport AST node.
 */
type ClassFileImportType = ASTType<typeof ClassFileImport>;

/**
 * Input data collected from the first dialog step (file selection).
 */
interface FileSelectionInputs {
    metamodelFile: string;
}

/**
 * Input data collected from the second dialog step (class selection).
 */
interface ClassSelectionInputs {
    className: string;
    alias?: string;
}

/**
 * Type guard to check if a value is FileSelectionInputs.
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
 * Type guard to check if a value is ClassSelectionInputs.
 */
function isClassSelectionInputs(value: unknown): value is ClassSelectionInputs {
    return typeof value === "object" && value !== null && "className" in value;
}

/**
 * Handler for the "import-class" action in the Metamodel language.
 *
 * This handler provides a two-step dialog:
 * 1. Select the metamodel file to import from
 * 2. Select the class and optional alias
 */
export class ImportClassActionHandler implements ActionHandler {
    /**
     * Shared Langium services for accessing workspace documents.
     */
    private readonly sharedServices: LangiumSharedServices;

    /**
     * Workspace edit service for creating edits.
     */
    private readonly workspaceEditService: WorkspaceEditService;

    /**
     * Creates an import class action handler.
     *
     * @param services Langium services with workspace edit support
     */
    constructor(services: LangiumLSPServices & WorkspaceEditAdditionalServices) {
        this.sharedServices = services.shared;
        this.workspaceEditService = services.workspace.WorkspaceEdit;
    }

    /**
     * Starts the import class action dialog.
     *
     * @param params The action start parameters with file path data
     * @returns The first dialog page with metamodel file selection
     */
    async startAction(params: ActionStartParams): Promise<ActionStartResponse> {
        const data = params.data as FileMenuActionData;
        const currentFilePath = URI.parse(data.uri).path;
        const metamodelFiles = await this.findMetamodelFiles(currentFilePath);
        const options = this.buildFileOptions(currentFilePath, metamodelFiles);

        return this.createFileSelectionPage(options);
    }

    /**
     * Submits form data and returns the next page or completion.
     *
     * @param params The submit parameters with collected inputs
     * @returns Validation errors, next page, or completion with workspace edit
     */
    async submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        const data = params.config.data as FileMenuActionData;
        const inputCount = params.inputs.length;

        if (inputCount === 1) {
            return this.handleFileSelection(params, data.uri);
        }

        return this.handleClassSelection(params, data.uri);
    }

    /**
     * Handles the first dialog step: file selection.
     *
     * @param params The submit parameters
     * @param currentFileUri The URI of the current file
     * @returns The second dialog page with class selection
     */
    private async handleFileSelection(
        params: ActionSubmitParams,
        currentFileUri: string
    ): Promise<ActionSubmitResponse> {
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

        const classes = await this.loadClassesFromFile(selectedFile, currentFileUri);

        if (classes.length === 0) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodelFile", message: "No classes found in the selected file" }]
            };
        }

        return this.createClassSelectionPage(classes);
    }

    /**
     * Handles the second dialog step: class selection and import creation.
     *
     * @param params The submit parameters
     * @param currentFileUri The URI of the current file
     * @returns Completion response with workspace edit
     */
    private async handleClassSelection(
        params: ActionSubmitParams,
        currentFileUri: string
    ): Promise<ActionSubmitResponse> {
        const validationResult = this.validateClassSelectionInputs(params);
        if (validationResult !== null) {
            return validationResult;
        }

        const { selectedFile, selectedClass, alias } = this.extractClassSelectionData(params);

        const workspaceEdit = await this.createImportEdit(currentFileUri, selectedFile, selectedClass, alias);
        const connection = this.sharedServices.lsp.Connection;
        await connection?.workspace.applyEdit(workspaceEdit);

        return { kind: "completion" };
    }

    /**
     * Validates the inputs from both dialog steps.
     */
    private validateClassSelectionInputs(params: ActionSubmitParams): ActionSubmitResponse | null {
        const rawFileInputs = params.inputs[0];
        const rawClassInputs = params.inputs[1];

        if (!isFileSelectionInputs(rawFileInputs)) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodelFile", message: "Invalid file selection data" }]
            };
        }

        if (!isClassSelectionInputs(rawClassInputs)) {
            return {
                kind: "validation",
                errors: [{ path: "/className", message: "Invalid class selection data" }]
            };
        }

        if (!rawClassInputs.className) {
            return {
                kind: "validation",
                errors: [{ path: "/className", message: "Please select a class to import" }]
            };
        }

        return null;
    }

    /**
     * Extracts validated input data from the submit parameters.
     *
     * @param params The submit parameters with inputs
     * @returns The extracted file, class, and alias data
     */
    private extractClassSelectionData(params: ActionSubmitParams): {
        selectedFile: string;
        selectedClass: string;
        alias: string;
    } {
        const fileInputs = params.inputs[0];
        const classInputs = params.inputs[1];

        if (!isFileSelectionInputs(fileInputs)) {
            throw new Error("Invalid file selection inputs");
        }
        if (!isClassSelectionInputs(classInputs)) {
            throw new Error("Invalid class selection inputs");
        }

        return {
            selectedFile: fileInputs.metamodelFile,
            selectedClass: classInputs.className ?? "",
            alias: typeof classInputs.alias === "string" ? classInputs.alias : ""
        };
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
            title: "Import Class",
            description: "Select the metamodel file to import from",
            schema,
            isLastPage: false,
            submitButtonLabel: "Next"
        };

        return { kind: "page", page };
    }

    /**
     * Creates the class selection dialog page.
     *
     * @param classes The list of available class names
     * @returns The dialog page response
     */
    private createClassSelectionPage(classes: string[]): ActionSubmitResponse {
        const schema: ActionSchema = {
            properties: {
                className: {
                    enum: classes,
                    placeholder: "Select a class to import"
                },
                alias: {
                    type: "string",
                    placeholder: "Optional alias (leave empty for no alias)"
                }
            },
            propertyLabels: {
                className: "Class",
                alias: "Alias"
            }
        };

        const page: ActionDialogPage = {
            title: "Import Class",
            description: "Select the class to import and optionally specify an alias",
            schema,
            isLastPage: true,
            submitButtonLabel: "Import"
        };

        return { kind: "nextPage", page };
    }

    /**
     * Loads the classes from a metamodel file.
     *
     * @param relativePath The relative path to the metamodel file
     * @param currentFileUri The URI of the current file
     * @returns Array of class names from the file
     */
    private async loadClassesFromFile(relativePath: string, currentFileUri: string): Promise<string[]> {
        const absolutePath = this.resolveRelativePath(currentFileUri, relativePath);
        const document = this.findDocumentByPath(absolutePath);

        if (document == undefined) {
            return [];
        }

        return this.extractClassNames(document);
    }

    /**
     * Finds a document by its absolute path.
     *
     * @param absolutePath The absolute path to look for
     * @returns The document if found, undefined otherwise
     */
    private findDocumentByPath(absolutePath: string): LangiumDocument | undefined {
        return this.sharedServices.workspace.LangiumDocuments.all
            .toArray()
            .find((doc: LangiumDocument) => doc.uri.path === absolutePath);
    }

    /**
     * Extracts class names from a metamodel document.
     *
     * @param document The document to extract classes from
     * @returns Array of class names
     */
    private extractClassNames(document: LangiumDocument): string[] {
        const parseResult = document.parseResult;
        if (parseResult == undefined || parseResult.value == undefined) {
            return [];
        }

        const root = parseResult.value as MetaModelType;
        if (root.classesAndAssociations == undefined) {
            return [];
        }

        const classNames = root.classesAndAssociations
            .filter((item): item is ClassType => item.$type === "Class")
            .map((cls) => cls.name)
            .filter((name): name is string => name != undefined);

        return [...new Set(classNames)];
    }

    /**
     * Creates the workspace edit for importing a class.
     *
     * @param currentFileUri The URI of the current file
     * @param relativePath The relative path to the import file
     * @param className The name of the class to import
     * @param alias The optional alias for the import
     * @returns The workspace edit
     */
    private async createImportEdit(
        currentFileUri: string,
        relativePath: string,
        className: string,
        alias: string
    ): Promise<WorkspaceEdit> {
        const document = this.findCurrentDocument(currentFileUri);

        if (document == undefined) {
            throw new Error("Current document not found");
        }

        const root = document.parseResult?.value as MetaModelType;
        if (root == undefined) {
            throw new Error("Root element not found in current document");
        }

        const existingImport = this.findExistingImportForFile(root, relativePath);

        if (existingImport != undefined) {
            return this.createAddToExistingImportEdit(existingImport, className, alias, document);
        }

        return this.createNewImportEdit(root, relativePath, className, alias, document, currentFileUri);
    }

    /**
     * Finds the current document by URI.
     *
     * @param currentFileUri The URI of the current file
     * @returns The document if found
     */
    private findCurrentDocument(currentFileUri: string): LangiumDocument | undefined {
        const currentPath = URI.parse(currentFileUri).path;
        return this.sharedServices.workspace.LangiumDocuments.all
            .toArray()
            .find((doc: LangiumDocument) => doc.uri.path === currentPath);
    }

    /**
     * Finds an existing import statement for a given file path.
     *
     * @param root The metamodel root node
     * @param relativePath The relative path to check
     * @returns The existing import if found
     */
    private findExistingImportForFile(root: MetaModelType, relativePath: string): ClassFileImportType | undefined {
        if (root.imports == undefined) {
            return undefined;
        }
        return root.imports.find((imp) => imp.file === relativePath);
    }

    /**
     * Creates a workspace edit to add a class to an existing import statement.
     *
     * @param existingImport The existing import statement
     * @param className The class name to add
     * @param alias The optional alias
     * @param document The document being edited
     * @returns The workspace edit
     */
    private async createAddToExistingImportEdit(
        existingImport: ClassFileImportType,
        className: string,
        alias: string,
        document: LangiumDocument
    ): Promise<WorkspaceEdit> {
        const cstNode = existingImport.$cstNode;
        if (cstNode == undefined) {
            throw new Error("CST node not found for existing import");
        }

        const newImport = this.buildNewImportNode(existingImport, className, alias);
        return this.workspaceEditService.replaceCstNode(cstNode, newImport, document);
    }

    /**
     * Builds a new import node with an additional class.
     *
     * @param existingImport The existing import to extend
     * @param className The class name to add
     * @param alias The optional alias
     * @returns The new import node
     */
    private buildNewImportNode(
        existingImport: ClassFileImportType,
        className: string,
        alias: string
    ): ClassFileImportType {
        const existingImports = existingImport.imports ?? [];
        const newClassImport = this.buildClassImport(className, alias);

        return {
            $type: ClassFileImport.name,
            file: existingImport.file,
            imports: [
                ...existingImports.map((imp: ClassImportType) => ({
                    $type: imp.$type,
                    entity: {
                        $refText: imp.entity.$refText,
                        ref: imp.entity.ref
                    },
                    name: imp.name
                })),
                newClassImport
            ]
        };
    }

    /**
     * Builds a class import entry.
     *
     * @param className The class name
     * @param alias The optional alias
     * @returns The class import object
     */
    private buildClassImport(className: string, alias: string) {
        return {
            $type: "ClassImport",
            entity: {
                $refText: className,
                ref: undefined
            },
            name: alias === "" ? undefined : alias
        };
    }

    /**
     * Creates a workspace edit for a new import statement.
     *
     * @param root The metamodel root node
     * @param relativePath The relative path to import from
     * @param className The class name to import
     * @param alias The optional alias
     * @param document The document being edited
     * @param currentFileUri The URI of the current file
     * @returns The workspace edit
     */
    private async createNewImportEdit(
        root: MetaModelType,
        relativePath: string,
        className: string,
        alias: string,
        document: LangiumDocument,
        currentFileUri: string
    ): Promise<WorkspaceEdit> {
        const importStatement = await this.buildImportStatement(relativePath, className, alias, document);

        if (root.imports != undefined && root.imports.length > 0) {
            return this.insertAfterLastImport(root.imports, importStatement, document);
        }

        return this.insertAtBeginning(importStatement, currentFileUri);
    }

    /**
     * Builds an import statement using AST node creation and serialization.
     *
     * @param relativePath The relative path to import from
     * @param className The class name to import
     * @param alias The optional alias
     * @param document The document being edited
     * @returns The formatted import statement
     */
    private async buildImportStatement(
        relativePath: string,
        className: string,
        alias: string,
        document: LangiumDocument
    ): Promise<string> {
        const classImport = this.buildClassImport(className, alias);

        const importNode: ClassFileImportType = {
            $type: ClassFileImport.name,
            file: relativePath,
            imports: [classImport]
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
        imports: ClassFileImportType[],
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
     * Resolves a relative path to an absolute path.
     *
     * @param baseUri The base URI
     * @param relativePath The relative path to resolve
     * @returns The absolute path
     */
    private resolveRelativePath(baseUri: string, relativePath: string): string {
        const basePath = URI.parse(baseUri).path;
        const baseParts = basePath.split("/");
        baseParts.pop();

        const relativeParts = relativePath.split("/");

        for (const part of relativeParts) {
            if (part === "." || part === "") {
                continue;
            } else if (part === "..") {
                baseParts.pop();
            } else {
                baseParts.push(part);
            }
        }

        return baseParts.join("/");
    }
}
