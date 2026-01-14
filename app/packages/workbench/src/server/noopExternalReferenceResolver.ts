import type { ExternalReferenceResolver } from "@mdeo/language-common";
import type { URI } from "langium";

/**
 * A no-operation external reference resolver that throws an error when attempting to load an external document.
 */
export class NoopExternalReferenceResolver implements ExternalReferenceResolver {
    loadExternalDocument(uri: URI): Promise<void> {
        throw new Error(`External reference ${uri.toString()} loading is not supported.`);
    }
}
