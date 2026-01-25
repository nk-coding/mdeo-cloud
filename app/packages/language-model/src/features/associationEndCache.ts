import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import { sharedImport } from "@mdeo/language-shared";
import type { LangiumSharedCoreServices, URI, DocumentCache as DocumentCacheType } from "langium";
import {
    Association,
    Class,
    ClassOrEnumImport,
    MetaModel,
    type AssociationEndType,
    type AssociationType,
    type ClassOrImportType,
    type ClassType,
    type MetaModelType
} from "@mdeo/language-metamodel";

const { DocumentCache, DocumentState, AstUtils } = sharedImport("langium");

/**
 * Cache service that maps class types to their associated association ends.
 *
 * This service builds a lookup table from class types to all association ends
 * that reference them. Since this computation is expensive (requires traversing
 * all associations in the metamodel), we use Langium's DocumentCache to cache
 * results per document.
 *
 * The cache automatically invalidates when metamodel documents change.
 */
export class AssociationEndCache {
    /**
     * Cache mapping from (document URI, class) to array of association ends.
     * Keyed by document to ensure cache invalidation when documents change.
     */
    private readonly cache: DocumentCacheType<ClassType, AssociationEndType[]>;

    /**
     * AST reflection for type checking.
     */
    private readonly astReflection: AstReflection;

    /**
     * Shared services for accessing workspace documents.
     */
    private readonly sharedServices: LangiumSharedCoreServices;

    /**
     * Constructs a new AssociationEndCache.
     *
     * @param services The extended Langium services
     */
    constructor(services: ExtendedLangiumServices) {
        this.astReflection = services.shared.AstReflection;
        this.sharedServices = services.shared;

        // Create a document-based cache that invalidates when documents are indexed
        // We use DocumentState.Linked to ensure the cache is cleared after references are resolved
        this.cache = new DocumentCache(services.shared, DocumentState.Linked);
    }

    /**
     * Gets all association ends that reference the given class type.
     *
     * This method:
     * 1. Finds the metamodel document containing the class
     * 2. Extracts all associations from that metamodel
     * 3. Returns association ends whose target class matches the input class
     *
     * Results are cached per document to avoid repeated computation.
     *
     * @param classOrImport The class or class import to find association ends for
     * @returns Array of association ends that reference this class
     */
    getAssociationEndsForClass(classOrImport: ClassOrImportType): AssociationEndType[] {
        // Resolve class import to actual class
        const targetClass = this.resolveClass(classOrImport);
        if (!targetClass) {
            return [];
        }

        // Get the document containing the class
        const document = AstUtils.getDocument(targetClass);
        if (!document) {
            return [];
        }

        // Try to get from cache first
        const cachedResult = this.cache.get(document.uri, targetClass);
        if (cachedResult !== undefined) {
            return cachedResult;
        }

        // Compute and cache the result
        const associationEnds = this.computeAssociationEndsForClass(targetClass, document.uri);
        this.cache.set(document.uri, targetClass, associationEnds);

        return associationEnds;
    }

    /**
     * Computes all association ends that reference the given class.
     *
     * This method traverses all associations in the class's metamodel document
     * and collects association ends whose target class matches the input class.
     *
     * @param targetClass The class to find association ends for
     * @param documentUri The URI of the metamodel document
     * @returns Array of association ends that reference this class
     */
    private computeAssociationEndsForClass(targetClass: ClassType, documentUri: URI): AssociationEndType[] {
        const result: AssociationEndType[] = [];

        // Get the metamodel document
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(documentUri);
        if (!document || !document.parseResult || !document.parseResult.value) {
            return result;
        }

        // Get the metamodel root
        const metamodel = document.parseResult.value;
        if (!this.astReflection.isInstance(metamodel, MetaModel)) {
            return result;
        }

        // Extract all associations from the metamodel
        const associations = this.extractAssociations(metamodel);

        // For each association, check if any end references our target class
        for (const association of associations) {
            // Check source end
            if (association.source?.class?.ref) {
                const sourceClass = this.resolveClass(association.source.class.ref);
                if (sourceClass === targetClass && association.source.name) {
                    result.push(association.source);
                }
            }

            // Check target end
            if (association.target?.class?.ref) {
                const targetClassRef = this.resolveClass(association.target.class.ref);
                if (targetClassRef === targetClass && association.target.name) {
                    result.push(association.target);
                }
            }
        }

        return result;
    }

    /**
     * Extracts all associations from a metamodel.
     *
     * @param metamodel The metamodel to extract associations from
     * @returns Array of associations
     */
    private extractAssociations(metamodel: MetaModelType): AssociationType[] {
        const associations: AssociationType[] = [];

        for (const element of metamodel.elements ?? []) {
            if (element && this.astReflection.isInstance(element, Association)) {
                associations.push(element);
            }
        }

        return associations;
    }

    /**
     * Resolves a class or class import to the actual class.
     *
     * @param classOrImport The class or class import to resolve
     * @returns The resolved class, or undefined if resolution fails
     */
    private resolveClass(classOrImport: ClassOrImportType): ClassType | undefined {
        if (this.astReflection.isInstance(classOrImport, Class)) {
            return classOrImport;
        } else if (this.astReflection.isInstance(classOrImport, ClassOrEnumImport)) {
            const resolved = classOrImport.entity?.ref;
            if (resolved && this.astReflection.isInstance(resolved, Class)) {
                return resolved;
            }
        }
        return undefined;
    }

    /**
     * Clears the entire cache.
     * Useful for testing or manual cache invalidation.
     */
    clear(): void {
        this.cache.clear();
    }

    /**
     * Clears the cache for a specific document.
     *
     * @param documentUri The URI of the document to clear
     */
    clearDocument(documentUri: URI): void {
        this.cache.clear(documentUri);
    }
}
