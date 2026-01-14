import type { FileDataHandler, FileDataContext, FileDataResult } from "@mdeo/service-common";

/**
 * Handler for computing the AST of a metamodel file.
 * Parses the document, builds it, and serializes the AST to JSON.
 *
 * @param context - The file data context with path, content, and services
 * @returns Promise resolving to the file data result with serialized AST
 */
export const astHandler: FileDataHandler = async (context: FileDataContext): Promise<FileDataResult> => {
    const { uri, instance, services } = context;

    const document = await instance.buildDocument(uri);

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
