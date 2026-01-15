import type { ExternalReferenceResolver } from "@mdeo/language-common";

/**
 * A no-operation external reference resolver that throws an error when attempting to load an external document.
 */
export class NoopExternalReferenceResolver implements ExternalReferenceResolver {
    async loadExternalDocuments(): Promise<void> {
        // No-op
    }
}
