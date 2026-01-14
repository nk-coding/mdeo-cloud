import type { LangiumDocument, URI } from "langium";
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
