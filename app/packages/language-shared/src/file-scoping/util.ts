import type { LangiumDocument, LangiumDocuments, URI } from "langium";
import { sharedImport } from "../sharedImport.js";

const { UriUtils } = sharedImport("langium");

/**
 * Resolves a relative file path based on the current document's URI.
 *
 * @param document The current Langium document
 * @param file The relative file path to resolve
 * @returns The resolved absolute URI of the file
 */
export function resolveRelativePath(document: LangiumDocument, file: string): URI {
    const currentUri = document.uri;
    const dirname = UriUtils.dirname(currentUri);
    return UriUtils.joinPath(dirname, file);
}

/**
 * Resolves and loads a document from a relative path.
 *
 * @param fromDocument The source document from which the relative path is resolved
 * @param relativePath The relative file path to resolve
 * @param documents The Langium document registry
 * @returns The resolved Langium document, or undefined if not found
 */
export function resolveRelativeDocument(
    fromDocument: LangiumDocument,
    relativePath: string | undefined,
    documents: LangiumDocuments
): LangiumDocument | undefined {
    if (relativePath == undefined || relativePath.trim() === "") {
        return undefined;
    }

    const uri = resolveRelativePath(fromDocument, relativePath);
    return documents.getDocument(uri);
}
