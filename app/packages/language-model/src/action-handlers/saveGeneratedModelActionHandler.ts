import type {
    ActionDialogPage,
    ActionSchema,
    ActionSchemaFileSelectNode,
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    AstSerializer,
    AstSerializerAdditionalServices,
    FileMenuActionData
} from "@mdeo/language-common";
import { buildFileSelectTree, calculateRelativePath, sharedImport, type ActionHandler } from "@mdeo/language-shared";
import type { LangiumCoreServices, LangiumDocument } from "langium";
import type { LangiumSharedServices } from "langium/lsp";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { ModelData } from "../features/modelData.js";
import { buildEnumEntryMap, buildModelAst, createMinimalDocument } from "../features/modelDataToAst.js";

const { URI } = sharedImport("langium");

/**
 * Input data collected from the save-as-model dialog.
 */
interface SaveAsModelInputs {
    directory: string | undefined;
    filename: string;
}

/**
 * Type guard to check if a value is {@link SaveAsModelInputs}.
 *
 * @param value - The value to check.
 * @returns `true` if `value` is a valid {@link SaveAsModelInputs} object.
 */
function isSaveAsModelInputs(value: unknown): value is SaveAsModelInputs {
    return (
        typeof value === "object" &&
        value !== null &&
        "filename" in value &&
        typeof (value as SaveAsModelInputs).filename === "string" &&
        (!("directory" in value) ||
            (value as SaveAsModelInputs).directory === undefined ||
            typeof (value as SaveAsModelInputs).directory === "string")
    );
}

/**
 * Handler for the "save-as-model" action on generated model files.
 *
 * This handler:
 *  - Parses the generated model's JSON content to extract ModelData
 *  - Presents a dialog with directory selection and filename input
 *  - Validates that the target file does not already exist
 *  - Builds a synthetic Model AST from the ModelData
 *  - Serializes the AST using the model language's AstSerializer
 *  - Creates the new .m file via workspace edit
 */
export class SaveGeneratedModelActionHandler implements ActionHandler {
    private readonly sharedServices: LangiumSharedServices;

    /**
     * @param sharedServices - The shared Langium services.
     */
    constructor(sharedServices: LangiumSharedServices) {
        this.sharedServices = sharedServices;
    }

    /**
     * Starts the save-as-model action by presenting a directory/filename dialog.
     * Parses the source generated model and collects workspace file paths to
     * populate the directory tree.
     *
     * @param params - The action start parameters, including the source file URI.
     * @returns A dialog page for the user to choose a directory and filename, or
     *          an error response if the model could not be parsed.
     */
    async startAction(params: ActionStartParams): Promise<ActionStartResponse> {
        const data = params.data as FileMenuActionData;

        const modelData = this.parseGeneratedModelContent(data.uri);
        if (!modelData) {
            return {
                kind: "error",
                message: "Failed to parse generated model",
                description: "The generated model file could not be parsed."
            };
        }

        const filePaths = this.findRegularFilePaths();
        if (filePaths.length === 0) {
            return {
                kind: "error",
                message: "No files found",
                description: "No files were found in the workspace to determine directory structure."
            };
        }

        const { nodes, rootPath } = buildFileSelectTree(filePaths);

        return this.createDialogPage(nodes, rootPath);
    }

    /**
     * Submits the save-as-model action after the user has provided a filename
     * and an optional directory. Validates the inputs and delegates to
     * {@link performModelSave}. When no directory is selected, the file is
     * saved to the project's root files directory.
     *
     * @param params - The action submit parameters, including dialog inputs.
     * @returns A completion response on success, or a validation/error response
     *          if the inputs are invalid.
     */
    async submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        const data = params.config.data as FileMenuActionData;
        const rawInputs = params.inputs[0];

        if (!isSaveAsModelInputs(rawInputs)) {
            return {
                kind: "validation",
                errors: [{ path: "/directory", message: "Invalid input structure" }]
            };
        }

        const { directory, filename } = rawInputs;

        if (!filename || filename.trim().length === 0) {
            return {
                kind: "validation",
                errors: [{ path: "/filename", message: "Please enter a filename" }]
            };
        }

        const normalizedFilename = filename.endsWith(".m") ? filename : filename + ".m";

        if (normalizedFilename.includes("/") || normalizedFilename.includes("\\")) {
            return {
                kind: "validation",
                errors: [{ path: "/filename", message: "Filename must not contain path separators" }]
            };
        }

        return this.performModelSave(data, directory ?? "", normalizedFilename);
    }

    /**
     * Performs the actual model save after inputs have been validated.
     * Checks for an existing file at the target location, resolves the metamodel,
     * builds and serializes a Model AST, and applies a workspace edit to create
     * the new file.
     *
     * @param data - The file menu action data containing the source URI.
     * @param directory - The absolute target directory path, or an empty string
     *                    to save into the project's root files directory.
     * @param normalizedFilename - The (already normalized) target filename, ending in `.m`.
     * @returns A completion response on success, or a validation/error response
     *          if the save could not be performed.
     */
    private async performModelSave(
        data: FileMenuActionData,
        directory: string,
        normalizedFilename: string
    ): Promise<ActionSubmitResponse> {
        const sourceUri = URI.parse(data.uri);
        const projectId = sourceUri.path.substring(1).split("/")[0];
        const newFileUri = URI.file(`/${projectId}/files${directory}/${normalizedFilename}`);

        if (this.sharedServices.workspace.LangiumDocuments.hasDocument(newFileUri)) {
            return {
                kind: "validation",
                errors: [
                    {
                        path: "/filename",
                        message: `A file named "${normalizedFilename}" already exists in this directory`
                    }
                ]
            };
        }

        const modelData = this.parseGeneratedModelContent(data.uri);
        if (!modelData) {
            return {
                kind: "error",
                message: "Failed to parse generated model",
                description: "The generated model file could not be parsed."
            };
        }

        const metamodelAbsolutePath = this.resolveMetamodelPath(modelData.metamodelPath);
        if (!metamodelAbsolutePath) {
            return {
                kind: "error",
                message: "Metamodel not found",
                description: `Could not find metamodel "${modelData.metamodelPath}" in the workspace.`
            };
        }

        const metamodelDocs = this.findMetamodelDocuments();
        const enumEntryMap = buildEnumEntryMap(metamodelDocs);

        const withinFilesTargetPath = directory + "/" + normalizedFilename;

        const metamodelRelativePath = calculateRelativePath(withinFilesTargetPath, metamodelAbsolutePath);

        const modelAst = buildModelAst(modelData, metamodelRelativePath, enumEntryMap);
        const fileContent = await this.serializeModelAst(modelAst);

        const workspaceEdit = this.createFileWorkspaceEdit(newFileUri.toString(), fileContent);
        const connection = this.sharedServices.lsp.Connection;
        await connection?.workspace.applyEdit(workspaceEdit);

        return { kind: "completion" };
    }

    /**
     * Parses the JSON content from a generated model document.
     *
     * @param uri - The URI string of the generated model document.
     * @returns The parsed {@link ModelData}, or `undefined` if the document
     *          could not be found or its content could not be parsed.
     */
    private parseGeneratedModelContent(uri: string): ModelData | undefined {
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(URI.parse(uri));
        if (!document) {
            return undefined;
        }

        const root = document.parseResult.value as unknown as Record<string, unknown>;
        const content = root?.content;
        if (!content || typeof content !== "string") {
            return undefined;
        }

        try {
            return JSON.parse(content) as ModelData;
        } catch {
            return undefined;
        }
    }

    /**
     * Finds all regular file paths in the workspace (for building directory tree).
     * Only includes documents under the `/files` segment of their project path.
     *
     * @returns An array of absolute URI path strings for regular workspace files.
     */
    private findRegularFilePaths(): string[] {
        const documents = this.sharedServices.workspace.LangiumDocuments.all.toArray();
        const paths: string[] = [];

        for (const doc of documents) {
            const uriPath = doc.uri.path;
            const parts = uriPath.substring(1).split("/");
            if (parts[1] === "files") {
                paths.push(uriPath);
            }
        }

        return paths;
    }

    /**
     * Finds all metamodel documents in the workspace.
     *
     * @returns An array of {@link LangiumDocument} instances whose language ID is `"metamodel"`.
     */
    private findMetamodelDocuments(): LangiumDocument[] {
        return this.sharedServices.workspace.LangiumDocuments.all
            .filter((doc) => doc.textDocument.languageId === "metamodel")
            .toArray();
    }

    /**
     * Resolves the metamodel path from {@link ModelData} to an absolute project path.
     * Searches metamodel documents by filename match.
     *
     * @param metamodelPathFromData - The metamodel path as recorded in the model data.
     * @returns The absolute project-relative path to the metamodel file, or `undefined`
     *          if no matching metamodel document was found.
     */
    private resolveMetamodelPath(metamodelPathFromData: string): string | undefined {
        const metamodelDocs = this.findMetamodelDocuments();
        const targetBasename = metamodelPathFromData.split("/").pop() ?? metamodelPathFromData;

        for (const doc of metamodelDocs) {
            const docPath = doc.uri.path;
            const docBasename = docPath.split("/").pop();
            if (docBasename === targetBasename) {
                const parts = docPath.substring(1).split("/");
                if (parts[1] === "files") {
                    return "/" + parts.slice(2).join("/");
                }
                return "/" + parts.slice(1).join("/");
            }
        }

        return undefined;
    }

    /**
     * Serializes the synthetic Model AST using the model language's AstSerializer.
     *
     * @param modelAst - The model AST to serialize.
     * @returns The serialized text content of the model file.
     */
    private async serializeModelAst(modelAst: unknown): Promise<string> {
        const fakeUri = URI.parse("fake:///file.m");
        const modelServices = this.sharedServices.ServiceRegistry.getServices(fakeUri) as LangiumCoreServices &
            AstSerializerAdditionalServices;
        const serializer: AstSerializer = modelServices.AstSerializer;

        const fakeDocument = createMinimalDocument("fake:///document.m");

        return serializer.serializeNode(modelAst as any, fakeDocument, {
            insertSpaces: true,
            tabSize: 4
        });
    }

    /**
     * Creates the dialog page with directory select and filename input.
     *
     * @param tree - The file-select tree nodes representing the directory structure.
     * @param rootPath - The root path of the file-select tree.
     * @returns An {@link ActionStartResponse} containing the dialog page definition.
     */
    private createDialogPage(tree: ActionSchemaFileSelectNode[], rootPath: string): ActionStartResponse {
        const schema: ActionSchema = {
            properties: {
                directory: {
                    fileSelect: tree,
                    rootPath,
                    selectDirectory: true,
                    placeholder: "Select target directory (defaults to root)"
                },
                filename: {
                    type: "string",
                    placeholder: "e.g. myModel.m"
                }
            },
            propertyLabels: {
                directory: "Directory",
                filename: "Filename"
            }
        };

        const page: ActionDialogPage = {
            title: "Save as Model File",
            description: "Save this generated model as a regular model file (.m)",
            schema,
            isLastPage: true,
            submitButtonLabel: "Save"
        };

        return { kind: "page", page };
    }

    /**
     * Creates a workspace edit that creates a new file with the given content.
     * Uses `documentChanges` with a {@link CreateFile} operation followed by a
     * {@link TextDocumentEdit} to insert the initial content.
     *
     * @param uri - The URI string of the file to create.
     * @param content - The initial text content to write into the new file.
     * @returns A {@link WorkspaceEdit} that creates the file and inserts content.
     */
    private createFileWorkspaceEdit(uri: string, content: string): WorkspaceEdit {
        return {
            documentChanges: [
                {
                    kind: "create" as const,
                    uri,
                    options: { overwrite: false, ignoreIfExists: false }
                },
                {
                    textDocument: { uri, version: null },
                    edits: [
                        {
                            range: {
                                start: { line: 0, character: 0 },
                                end: { line: 0, character: 0 }
                            },
                            newText: content
                        }
                    ]
                }
            ]
        };
    }
}
