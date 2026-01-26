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
import { calculateRelativePath, type ActionHandler } from "@mdeo/language-shared";
import type { LangiumDocument } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { URI } from "langium";
import {
    FileImport,
    type ClassOrEnumImportType,
    type MetaModelType,
    Class,
    Enum,
    ClassOrEnumImport
} from "../grammar/metamodelTypes.js";
import type { LangiumLSPServices, LangiumSharedServices } from "langium/lsp";
import type { ASTType } from "@mdeo/language-common";

/**
 * Type for FileImport AST node.
 */
type FileImportType = ASTType<typeof FileImport>;

/**
 * Input data collected from the first dialog step (file selection).
 */
interface FileSelectionInputs {
    metamodelFile: string;
}

/**
 * Input data collected from the second dialog step (entity selection).
 */
interface EntitySelectionInputs {
    entityName: string;
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
 * Type guard to check if a value is EntitySelectionInputs.
 */
function isEntitySelectionInputs(value: unknown): value is EntitySelectionInputs {
    return typeof value === "object" && value !== null && "entityName" in value;
}

/**
 * Base abstract handler for importing entities (classes or enums) in the Metamodel language.
 *
 * This handler provides a two-step dialog:
 * 1. Select the metamodel file to import from
 * 2. Select the entity and optional alias
 *
 * Subclasses must implement entity-specific methods for filtering and import creation.
 */
export abstract class ImportEntityActionHandler implements ActionHandler {
    /**
     * Shared Langium services for accessing workspace documents.
     */
    protected readonly sharedServices: LangiumSharedServices;

    /**
     * Workspace edit service for creating edits.
     */
    protected readonly workspaceEditService: WorkspaceEditService;

    /**
     * AST reflection for type checking.
     */
    protected readonly reflection: AstReflection;

    /**
     * Creates an import entity action handler.
     *
     * @param services Langium services with workspace edit support
     */
    constructor(
        protected readonly services: LangiumLSPServices & WorkspaceEditAdditionalServices & ExtendedLangiumServices
    ) {
        this.sharedServices = services.shared;
        this.workspaceEditService = services.workspace.WorkspaceEdit;
        this.reflection = services.shared.AstReflection;
    }

    /**
     * Gets the entity type name for display purposes (e.g., "Class", "Enum").
     */
    protected abstract getEntityTypeName(): string;

    /**
     * Extracts entity names from a metamodel document.
     *
     * @param document The document to extract entities from
     * @returns Array of entity names
     */
    protected abstract extractEntityNames(document: LangiumDocument): string[];

    /**
     * Starts the import entity action dialog.
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

        return this.handleEntitySelection(params, data.uri);
    }

    /**
     * Handles the first dialog step: file selection.
     *
     * @param params The submit parameters
     * @param currentFileUri The URI of the current file
     * @returns The second dialog page with entity selection
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

        const entities = await this.loadEntitiesFromFile(selectedFile, currentFileUri);

        if (entities.length === 0) {
            return {
                kind: "validation",
                errors: [
                    {
                        path: "/metamodelFile",
                        message: `No ${this.getEntityTypeName().toLowerCase()}s found in the selected file`
                    }
                ]
            };
        }

        return this.createEntitySelectionPage(entities);
    }

    /**
     * Handles the second dialog step: entity selection and import creation.
     *
     * @param params The submit parameters
     * @param currentFileUri The URI of the current file
     * @returns Completion response with workspace edit
     */
    private async handleEntitySelection(
        params: ActionSubmitParams,
        currentFileUri: string
    ): Promise<ActionSubmitResponse> {
        const validationResult = await this.validateEntitySelectionInputs(params, currentFileUri);
        if (validationResult !== null) {
            return validationResult;
        }

        const { selectedFile, selectedEntity, alias } = this.extractEntitySelectionData(params);

        const workspaceEdit = await this.createImportEdit(currentFileUri, selectedFile, selectedEntity, alias);
        const connection = this.sharedServices.lsp.Connection;
        await connection?.workspace.applyEdit(workspaceEdit);

        return { kind: "completion" };
    }

    /**
     * Validates the inputs from both dialog steps.
     */
    private async validateEntitySelectionInputs(
        params: ActionSubmitParams,
        currentFileUri: string
    ): Promise<ActionSubmitResponse | null> {
        const rawFileInputs = params.inputs[0];
        const rawEntityInputs = params.inputs[1];

        if (!isFileSelectionInputs(rawFileInputs)) {
            return {
                kind: "validation",
                errors: [{ path: "/metamodelFile", message: "Invalid file selection data" }]
            };
        }

        if (!isEntitySelectionInputs(rawEntityInputs)) {
            return {
                kind: "validation",
                errors: [{ path: `/entityName`, message: "Invalid entity selection data" }]
            };
        }

        if (!rawEntityInputs.entityName) {
            return {
                kind: "validation",
                errors: [
                    {
                        path: `/entityName`,
                        message: `Please select a ${this.getEntityTypeName().toLowerCase()} to import`
                    }
                ]
            };
        }

        const nameToCheck =
            rawEntityInputs.alias && rawEntityInputs.alias.trim() !== ""
                ? rawEntityInputs.alias.trim()
                : rawEntityInputs.entityName;

        const nameConflict = await this.checkNameConflict(currentFileUri, nameToCheck);
        if (nameConflict !== null) {
            return {
                kind: "validation",
                errors: [
                    {
                        path: rawEntityInputs.alias && rawEntityInputs.alias.trim() !== "" ? "/alias" : `/entityName`,
                        message: nameConflict
                    }
                ]
            };
        }

        return null;
    }

    /**
     * Checks if a name conflicts with existing classes, enums, or imports in the current file.
     *
     * @param currentFileUri The URI of the current file
     * @param name The name to check
     * @returns An error message if there's a conflict, null otherwise
     */
    private async checkNameConflict(currentFileUri: string, name: string): Promise<string | null> {
        const document = this.findCurrentDocument(currentFileUri);

        if (document == undefined) {
            return null;
        }

        const exported = await this.services.references.ScopeComputation.collectExportedSymbols(document);
        const existing = exported.find((description) => description.name === name);
        if (existing == undefined) {
            return null;
        }
        if (existing.type === Class.name) {
            return `A class named "${name}" already exists in this file`;
        } else if (existing.type === Enum.name) {
            return `An enum named "${name}" already exists in this file`;
        } else if (existing.type === FileImport.name) {
            return `An import named "${name}" already exists in this file`;
        } else {
            throw new Error(`Unexpected exported symbol type: ${existing.type}`);
        }
    }

    /**
     * Extracts validated input data from the submit parameters.
     *
     * @param params The submit parameters with inputs
     * @returns The extracted file, entity, and alias data
     */
    private extractEntitySelectionData(params: ActionSubmitParams): {
        selectedFile: string;
        selectedEntity: string;
        alias: string;
    } {
        const fileInputs = params.inputs[0];
        const entityInputs = params.inputs[1];

        if (!isFileSelectionInputs(fileInputs)) {
            throw new Error("Invalid file selection inputs");
        }
        if (!isEntitySelectionInputs(entityInputs)) {
            throw new Error("Invalid entity selection inputs");
        }

        return {
            selectedFile: fileInputs.metamodelFile,
            selectedEntity: entityInputs.entityName ?? "",
            alias: typeof entityInputs.alias === "string" ? entityInputs.alias : ""
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
            title: `Import ${this.getEntityTypeName()}`,
            description: "Select the metamodel file to import from",
            schema,
            isLastPage: false,
            submitButtonLabel: "Next"
        };

        return { kind: "page", page };
    }

    /**
     * Creates the entity selection dialog page.
     *
     * @param entities The list of available entity names
     * @returns The dialog page response
     */
    private createEntitySelectionPage(entities: string[]): ActionSubmitResponse {
        const entityTypeName = this.getEntityTypeName();

        const schema: ActionSchema = {
            properties: {
                entityName: {
                    enum: entities,
                    placeholder: `Select a ${entityTypeName.toLowerCase()} to import`
                },
                alias: {
                    type: "string",
                    placeholder: "Optional alias (leave empty for no alias)"
                }
            },
            propertyLabels: {
                entityName: entityTypeName,
                alias: "Alias"
            }
        };

        const page: ActionDialogPage = {
            title: `Import ${entityTypeName}`,
            description: `Select the ${entityTypeName.toLowerCase()} to import and optionally specify an alias`,
            schema,
            isLastPage: true,
            submitButtonLabel: "Import"
        };

        return { kind: "nextPage", page };
    }

    /**
     * Loads the entities from a metamodel file.
     *
     * @param relativePath The relative path to the metamodel file
     * @param currentFileUri The URI of the current file
     * @returns Array of entity names from the file
     */
    private async loadEntitiesFromFile(relativePath: string, currentFileUri: string): Promise<string[]> {
        const absolutePath = this.resolveRelativePath(currentFileUri, relativePath);
        const document = this.findDocumentByPath(absolutePath);

        if (document == undefined) {
            return [];
        }

        return this.extractEntityNames(document);
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
     * Creates the workspace edit for importing an entity.
     *
     * @param currentFileUri The URI of the current file
     * @param relativePath The relative path to the import file
     * @param entityName The name of the entity to import
     * @param alias The optional alias for the import
     * @returns The workspace edit
     */
    private async createImportEdit(
        currentFileUri: string,
        relativePath: string,
        entityName: string,
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
            return this.createAddToExistingImportEdit(existingImport, entityName, alias, document);
        }

        return this.createNewImportEdit(root, relativePath, entityName, alias, document, currentFileUri);
    }

    /**
     * Finds the current document by URI.
     *
     * @param currentFileUri The URI of the current file
     * @returns The document if found
     */
    protected findCurrentDocument(currentFileUri: string): LangiumDocument | undefined {
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
    private findExistingImportForFile(root: MetaModelType, relativePath: string): FileImportType | undefined {
        if (root.imports == undefined) {
            return undefined;
        }
        const found = root.imports.find(
            (imp) => this.reflection.isInstance(imp, FileImport) && imp.file === relativePath
        );
        return found;
    }

    /**
     * Creates a workspace edit to add an entity to an existing import statement.
     *
     * @param existingImport The existing import statement
     * @param entityName The entity name to add
     * @param alias The optional alias
     * @param document The document being edited
     * @returns The workspace edit
     */
    private async createAddToExistingImportEdit(
        existingImport: FileImportType,
        entityName: string,
        alias: string,
        document: LangiumDocument
    ): Promise<WorkspaceEdit> {
        const cstNode = existingImport.$cstNode;
        if (cstNode == undefined) {
            throw new Error("CST node not found for existing import");
        }

        const newImport = this.buildNewImportNode(existingImport, entityName, alias);
        return this.workspaceEditService.replaceCstNode(cstNode, newImport, document);
    }

    /**
     * Builds a new import node with an additional entity.
     *
     * @param existingImport The existing import to extend
     * @param entityName The entity name to add
     * @param alias The optional alias
     * @returns The new import node
     */
    private buildNewImportNode(existingImport: FileImportType, entityName: string, alias: string): FileImportType {
        const existingImports = existingImport.imports ?? [];
        const newEntityImport = this.buildEntityImport(entityName, alias);

        return {
            $type: FileImport.name,
            file: existingImport.file,
            imports: [
                ...existingImports.map((imp: ClassOrEnumImportType) => ({
                    $type: imp.$type,
                    entity: {
                        $refText: imp.entity.$refText,
                        ref: imp.entity.ref
                    },
                    name: imp.name
                })),
                newEntityImport
            ]
        };
    }

    /**
     * Builds an entity import entry.
     *
     * @param entityName The entity name
     * @param alias The optional alias
     * @returns The entity import object
     */
    private buildEntityImport(entityName: string, alias: string): ClassOrEnumImportType {
        return {
            $type: ClassOrEnumImport.name,
            entity: {
                $refText: entityName,
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
     * @param entityName The entity name to import
     * @param alias The optional alias
     * @param document The document being edited
     * @param currentFileUri The URI of the current file
     * @returns The workspace edit
     */
    private async createNewImportEdit(
        root: MetaModelType,
        relativePath: string,
        entityName: string,
        alias: string,
        document: LangiumDocument,
        currentFileUri: string
    ): Promise<WorkspaceEdit> {
        const importStatement = await this.buildImportStatement(relativePath, entityName, alias, document);

        if (root.imports != undefined && root.imports.length > 0) {
            const fileImports = root.imports.filter((imp) => this.reflection.isInstance(imp, FileImport));
            if (fileImports.length > 0) {
                return this.insertAfterLastImport(fileImports, importStatement, document);
            }
        }

        return this.insertAtBeginning(importStatement, currentFileUri);
    }

    /**
     * Builds an import statement using AST node creation and serialization.
     *
     * @param relativePath The relative path to import from
     * @param entityName The entity name to import
     * @param alias The optional alias
     * @param document The document being edited
     * @returns The formatted import statement
     */
    private async buildImportStatement(
        relativePath: string,
        entityName: string,
        alias: string,
        document: LangiumDocument
    ): Promise<string> {
        const entityImport = this.buildEntityImport(entityName, alias);

        const importNode: FileImportType = {
            $type: FileImport.name,
            file: relativePath,
            imports: [entityImport]
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
