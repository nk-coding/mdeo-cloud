import type { LangiumDocument, WorkspaceCache as WorkspaceCacheType } from "langium";
import { sharedImport } from "@mdeo/language-shared";

const { WorkspaceCache, DocumentState } = sharedImport("langium");

/**
 * Cached per-document package map state, computed once per document lifecycle.
 */
export type DocumentPackageCache = {
    /**
     * Map from user-visible package names to lists of internal packages.
     */
    packageMap: Map<string, string[]>;
    /**
     * Set of all internal packages from all values of packageMap.
     */
    allInternalPackages: Set<string>;
};

/**
 * Service that computes and caches the package map for each document.
 * The package map maps user-visible package names (e.g., "class", "enum", "builtin")
 * to lists of internal package identifiers.
 */
export interface DocumentPackageCacheService {
    /**
     * Retrieves (or computes and caches) the package map state for the given document.
     *
     * @param document The document for which to compute the package map
     * @returns The cached DocumentPackageCache for that document
     */
    getDocumentPackageCache(document: LangiumDocument): DocumentPackageCache;
}

/**
 * Default implementation of {@link DocumentPackageCacheService} that uses Langium's
 * {@link WorkspaceCache} to cache results per document, invalidating at
 * {@link DocumentState.IndexedReferences}.
 */
export class DefaultDocumentPackageCacheService implements DocumentPackageCacheService {
    private readonly cache: WorkspaceCacheType<string, DocumentPackageCache>;

    /**
     * Creates a new DocumentPackageCacheService.
     *
     * @param langiumServices The Langium shared services, used for workspace cache invalidation
     * @param computePackageMap Language-specific callback that computes the package map for a document
     */
    constructor(
        langiumServices: { workspace: unknown },
        private readonly computePackageMap: (document: LangiumDocument) => Map<string, string[]>
    ) {
        this.cache = new WorkspaceCache(langiumServices as any, DocumentState.IndexedReferences);
    }

    getDocumentPackageCache(document: LangiumDocument): DocumentPackageCache {
        const key = document.uri.toString();
        const existing = this.cache.get(key);
        if (existing != undefined) {
            return existing;
        }
        const packageMap = this.computePackageMap(document);
        const allInternalPackages = new Set<string>();
        for (const internalPackages of packageMap.values()) {
            for (const pkg of internalPackages) {
                allInternalPackages.add(pkg);
            }
        }
        const entry: DocumentPackageCache = { packageMap, allInternalPackages };
        this.cache.set(key, entry);
        return entry;
    }
}

/**
 * No-op implementation of {@link DocumentPackageCacheService} used as the default
 * when no language-specific package map is provided.
 * Always returns empty maps, effectively disabling type annotation features.
 */
export class NoopDocumentPackageCacheService implements DocumentPackageCacheService {
    getDocumentPackageCache(_document: LangiumDocument): DocumentPackageCache {
        return { packageMap: new Map(), allInternalPackages: new Set() };
    }
}
