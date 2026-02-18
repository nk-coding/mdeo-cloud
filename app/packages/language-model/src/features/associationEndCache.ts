import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import { sharedImport } from "@mdeo/language-shared";
import type { LangiumSharedCoreServices, URI, DocumentCache as DocumentCacheType } from "langium";
import {
    Association,
    MetaModel,
    type AssociationEndType,
    type AssociationType,
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
     * @param classType The class to find association ends for
     * @returns Array of association ends that reference this class
     */
    getAssociationEndsForClass(classType: ClassType): AssociationEndType[] {
        const document = AstUtils.getDocument(classType);
        if (!document) {
            return [];
        }

        const cachedResult = this.cache.get(document.uri, classType);
        if (cachedResult !== undefined) {
            return cachedResult;
        }

        const associationEnds = this.computeAssociationEndsForClass(classType, document.uri);
        this.cache.set(document.uri, classType, associationEnds);

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

        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(documentUri);
        if (!document || !document.parseResult || !document.parseResult.value) {
            return result;
        }

        const metamodel = document.parseResult.value;
        if (!this.astReflection.isInstance(metamodel, MetaModel)) {
            return result;
        }

        const associations = this.extractAssociations(metamodel);

        for (const association of associations) {
            const sourceClass = association.source?.class?.ref as ClassType | undefined;
            if (sourceClass === targetClass && association.source.name) {
                result.push(association.source);
            }

            const targetClassRef = association.target?.class?.ref as ClassType | undefined;
            if (targetClassRef === targetClass && association.target.name) {
                result.push(association.target);
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
