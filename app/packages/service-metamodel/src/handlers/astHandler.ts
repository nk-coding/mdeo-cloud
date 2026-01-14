import type { FileDataHandler, FileDataContext, FileDataResult } from "@mdeo/service-common";

/**
 * Handler for computing the AST of a metamodel file.
 * Parses the document, builds it, and serializes the AST to JSON.
 *
 * @param context - The file data context with path, content, and services
 * @returns Promise resolving to the file data result with serialized AST
 */
export const astHandler: FileDataHandler = async (context: FileDataContext): Promise<FileDataResult> => {
    const { path, content, services } = context;

    // Parse the document
    const { URI } = await import("vscode-uri");
    const uri = URI.parse(`file://${path}`);

    // Create a temporary document for parsing
    const document = services.shared.workspace.LangiumDocumentFactory.fromString(content, uri);

    // Build the document (parse, link, validate)
    await services.shared.workspace.DocumentBuilder.build([document], { validation: true });

    // Get the AST
    const ast = document.parseResult.value;

    // Serialize the AST to JSON
    const serializedAst = JSON.stringify(
        ast,
        (key, value) => {
            // Skip circular references and internal properties
            if (key.startsWith("$") || key === "$container" || key === "$document") {
                return undefined;
            }
            return value;
        },
        2
    );

    // TODO: Cleanup - remove document from workspace

    return {
        data: serializedAst,
        fileDependencies: [],
        dataDependencies: [],
        additionalFileData: []
    };
};
