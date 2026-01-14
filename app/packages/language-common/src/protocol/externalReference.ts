import type { LangiumDocument, URI } from "langium";

/**
 * Additional services for external reference handling.
 */
export interface ExternalReferenceAdditionalServices {
    references: {
        ExternalReferenceCollector: ExternalReferenceCollector;
    };
}

/**
 * Additional shared services for external reference handling.
 */
export interface ExternalReferenceSharedAdditionalServices {
    references: {
        ExternalReferenceResolver: ExternalReferenceResolver;
    };
}

/**
 * A collector for external references used in reference resolution.
 */
export interface ExternalReferenceCollector {
    /**
     * Finds external references required for the given documents.
     * May include any of these documents in the result.
     *
     * @param docs The documents to find external references for.
     * @return The URIs of the external references.
     */
    findExternalReferences(docs: LangiumDocument[]): URI[];
}

/**
 * A resolver for external references used in reference resolution.
 */
export interface ExternalReferenceResolver {
    /**
     * Loads an external document by its URI.
     *
     * @param uri The URI of the external document to load.
     * @return a promise that indicates whether some document was loaded
     */
    loadExternalDocument(uri: URI): Promise<void>;
}
