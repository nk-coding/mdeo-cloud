import type { ExternalReferenceResolver } from "@mdeo/language-common";

/**
 * A no-operation external reference resolver that throws an error when attempting to load an external document.
 */
export class NoopExternalReferenceResolver implements ExternalReferenceResolver {
    async loadExternalDocument(): Promise<void> {
        // No-op
    }
}
