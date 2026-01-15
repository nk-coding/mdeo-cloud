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
     * @return The copllected external references.
     */
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences;
}

/**
 * The result of collecting external references.
 */
export interface ExternalReferences {
    /**
     * The URIs of local references (where files should be loaded directly).
     */
    local: URI[];
    /**
     * The URIs of external references (where exports should be used)
     */
    external: URI[];
}

/**
 * A resolver for external references used in reference resolution.
 */
export interface ExternalReferenceResolver {
    /**
     * Loads an external document by its URI.
     *
     * @param externalReferences The external references to load
     * @return a promise that indicates whether some document was loaded
     */
    loadExternalDocuments(externalReferences: ExternalReferences): Promise<void>;
}
