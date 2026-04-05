import type { LangiumDocument, LangiumCoreServices, LangiumSharedCoreServices } from "langium";
import { DefaultDocumentBuilder, DocumentState } from "langium";
import type { ExternalReferenceAdditionalServices, ExtendedDocumentBuilder } from "@mdeo/language-common";

/**
 * A document builder that tracks external reference dependencies and uses them
 * to determine which documents need relinking when a file changes.
 *
 * This enhances Langium's built-in `isAffected()` check (which uses the reference index)
 * with explicit dependency information from the ExternalReferenceCollector system.
 * This ensures that cross-language dependencies (e.g., config → metamodel) are always
 * captured, even if no direct Langium cross-references exist.
 */
export class DependencyAwareDocumentBuilder extends DefaultDocumentBuilder implements ExtendedDocumentBuilder {
    /**
     * Forward dependency map: document URI → set of dependency URIs.
     */
    private readonly forwardDeps = new Map<string, Set<string>>();

    /**
     * Inverse dependency map: dependency URI → set of dependent document URIs.
     */
    private readonly inverseDeps = new Map<string, Set<string>>();

    private readonly sharedServices: LangiumSharedCoreServices;

    constructor(services: LangiumSharedCoreServices) {
        super(services);
        this.sharedServices = services;
        this.setupDependencyTracking();
    }

    /**
     * Registers build-phase listeners so that dependency information is automatically
     * updated whenever documents are linked or deleted, without any external setup call.
     */
    private setupDependencyTracking(): void {
        this.onBuildPhase(DocumentState.Linked, async (docs) => {
            for (const doc of docs) {
                const languageId = doc.textDocument.languageId;
                const langServices = this.sharedServices.ServiceRegistry.all.find(
                    (s) => s.LanguageMetaData.languageId === languageId
                ) as (LangiumCoreServices & Partial<ExternalReferenceAdditionalServices>) | undefined;
                const collector = langServices?.references?.ExternalReferenceCollector;
                if (!collector) {
                    continue;
                }
                const refs = collector.findExternalReferences([doc]);
                const depUris = [...refs.local, ...refs.external].map((uri) => uri.toString());
                this.setDependencies(doc.uri.toString(), depUris);
            }
        });

        this.onUpdate(async (_changed, deleted) => {
            for (const deletedUri of deleted) {
                this.removeDependencies(deletedUri.toString());
            }
        });
    }

    /**
     * Updates the dependency information for a document based on its external references.
     *
     * @param docUri The URI of the document
     * @param dependencyUris The URIs of files this document depends on
     */
    setDependencies(docUri: string, dependencyUris: string[]): void {
        const oldDeps = this.forwardDeps.get(docUri);
        if (oldDeps) {
            for (const dep of oldDeps) {
                this.inverseDeps.get(dep)?.delete(docUri);
            }
        }

        const newDeps = new Set(dependencyUris);
        this.forwardDeps.set(docUri, newDeps);

        for (const dep of newDeps) {
            let dependents = this.inverseDeps.get(dep);
            if (!dependents) {
                dependents = new Set();
                this.inverseDeps.set(dep, dependents);
            }
            dependents.add(docUri);
        }
    }

    /**
     * Returns all transitive forward dependencies of a document (what it depends on, recursively).
     *
     * @param docUri The URI string of the document
     * @returns Array of URI strings for all transitive dependencies (excluding the document itself)
     */
    getTransitiveDependencies(docUri: string): string[] {
        const visited = new Set<string>();
        const toVisit = [docUri];
        while (toVisit.length > 0) {
            const current = toVisit.pop()!;
            if (visited.has(current)) continue;
            visited.add(current);
            for (const dep of this.forwardDeps.get(current) ?? []) {
                toVisit.push(dep);
            }
        }
        visited.delete(docUri);
        return [...visited];
    }

    /**
     * Removes all dependency information for a document.
     */
    removeDependencies(docUri: string): void {
        const deps = this.forwardDeps.get(docUri);
        if (deps) {
            for (const dep of deps) {
                this.inverseDeps.get(dep)?.delete(docUri);
            }
        }
        this.forwardDeps.delete(docUri);
    }

    /**
     * Overrides the default `shouldRelink` to also check external reference dependencies.
     * A document should be relinked if:
     * 1. It has any linking errors (Langium default behavior)
     * 2. It is affected according to the reference index (Langium default behavior)
     * 3. It depends on any changed URI via external references (our enhancement)
     */
    protected override shouldRelink(document: LangiumDocument, changedUris: Set<string>): boolean {
        if (super.shouldRelink(document, changedUris)) {
            return true;
        }
        const deps = this.forwardDeps.get(document.uri.toString());
        if (deps) {
            for (const dep of deps) {
                if (changedUris.has(dep)) {
                    return true;
                }
            }
        }
        return false;
    }
}
