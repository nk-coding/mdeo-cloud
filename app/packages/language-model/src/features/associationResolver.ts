import type { AstReflection } from "@mdeo/language-common";
import {
    Association,
    resolveClassChain,
    type AssociationType,
    type ClassType,
    type AssociationEndType
} from "@mdeo/language-metamodel";
import { BaseMetamodelHelper } from "./baseMetamodelHelper.js";

/**
 * Result of resolving an association between two classes.
 */
export interface ResolvedAssociation {
    /**
     * The association that connects the two classes.
     */
    association: AssociationType;

    /**
     * Whether the link direction matches the association direction.
     * True if source class matches association source, false if reversed.
     */
    matchesDirection: boolean;
}

/**
 * Helper class for resolving associations between classes in metamodels.
 *
 * Extends {@link BaseMetamodelHelper} for shared metamodel traversal utilities
 * and adds methods to:
 * - Find a unique association between two classes
 * - Look up an association by one of its end properties
 * - Resolve the oriented association for a link, determining directionality
 */
export class AssociationResolver extends BaseMetamodelHelper {
    /**
     * Creates a new AssociationResolver.
     *
     * @param reflection The AST reflection for type checking
     */
    constructor(reflection: AstReflection) {
        super(reflection);
    }

    /**
     * Finds the unique association between two classes.
     * Returns undefined if there are zero or multiple associations.
     *
     * @param sourceClass The source class of the link
     * @param targetClass The target class of the link
     * @returns The unique association, or undefined if not unique
     */
    findUniqueAssociation(sourceClass: ClassType, targetClass: ClassType): AssociationType | undefined {
        const associations = this.findAssociationsBetweenClasses(sourceClass, targetClass);
        return associations.length === 1 ? associations[0] : undefined;
    }

    /**
     * Finds the association that owns the given association end.
     *
     * @param associationend The association end whose containing association to find
     * @returns The containing association, or undefined if not found
     */
    findAssociationForAssociationEnd(associationend: AssociationEndType): AssociationType | undefined {
        const container = associationend.$container;
        if (container && this.reflection.isInstance(container, Association)) {
            return container;
        }
        return undefined;
    }

    /**
     * Resolves an association and determines its directionality for a link.
     *
     * Handles three cases:
     * 1. Both association ends are given: validates they belong to the same association.
     * 2. One association end is given: resolves the association and determines direction.
     * 3. No association ends are given: finds the unique association and determines direction.
     *
     * @param sourceClass The source class of the link
     * @param targetClass The target class of the link
     * @param sourceAssociationEnd The association end on the source side, if specified
     * @param targetAssociationEnd The association end on the target side, if specified
     * @returns The resolved association with directionality, or undefined
     */
    resolveAssociation(
        sourceClass: ClassType,
        targetClass: ClassType,
        sourceAssociationEnd?: AssociationEndType,
        targetAssociationEnd?: AssociationEndType
    ): ResolvedAssociation | undefined {
        if (sourceAssociationEnd != undefined && targetAssociationEnd != undefined) {
            return this.resolveWithBothProperties(sourceAssociationEnd, targetAssociationEnd, sourceClass);
        }

        if (sourceAssociationEnd != undefined || targetAssociationEnd != undefined) {
            return this.resolveWithOneAssociationEnd(
                sourceClass,
                targetClass,
                sourceAssociationEnd ?? targetAssociationEnd!,
                sourceAssociationEnd != undefined
            );
        }

        return this.resolveWithNoProperties(sourceClass, targetClass);
    }

    /**
     * Resolves the association when both end properties are specified.
     *
     * @param sourceAssociationEnd The source-side association end
     * @param targetAssociationEnd The target-side association end
     * @param sourceClass The source class, used to verify directionality
     * @returns The resolved association with directionality, or undefined if the ends differ
     */
    private resolveWithBothProperties(
        sourceAssociationEnd: AssociationEndType,
        targetAssociationEnd: AssociationEndType,
        sourceClass: ClassType
    ): ResolvedAssociation | undefined {
        const sourceAssoc = this.findAssociationForAssociationEnd(sourceAssociationEnd);
        const targetAssoc = this.findAssociationForAssociationEnd(targetAssociationEnd);

        if (!sourceAssoc || !targetAssoc || sourceAssoc !== targetAssoc) {
            return undefined;
        }
        const matchesDirection = this.associationendMatchesAssociationSource(
            sourceAssociationEnd,
            sourceAssoc,
            sourceClass
        );

        return { association: sourceAssoc, matchesDirection };
    }

    /**
     * Resolves the association when only one end property is specified.
     *
     * @param sourceClass The source class of the link
     * @param targetClass The target class of the link
     * @param associationend The single specified association end
     * @param isSourceAssociationEnd Whether the specified end is on the source side
     * @returns The resolved association with directionality, or undefined if not found
     */
    private resolveWithOneAssociationEnd(
        sourceClass: ClassType,
        targetClass: ClassType,
        associationend: AssociationEndType,
        isSourceAssociationEnd: boolean
    ): ResolvedAssociation | undefined {
        const association = this.findAssociationForAssociationEnd(associationend);
        if (!association) {
            return undefined;
        }

        const matchesDirection = this.associationendMatchesAssociationSource(
            associationend,
            association,
            isSourceAssociationEnd ? sourceClass : targetClass
        );

        return { association, matchesDirection };
    }

    /**
     * Resolves the association when no end properties are specified.
     *
     * @param sourceClass The source class of the link
     * @param targetClass The target class of the link
     * @returns The unique resolved association with directionality, or undefined
     */
    private resolveWithNoProperties(sourceClass: ClassType, targetClass: ClassType): ResolvedAssociation | undefined {
        const association = this.findUniqueAssociation(sourceClass, targetClass);
        if (!association) {
            return undefined;
        }

        const assocSourceClass = association.source?.class?.ref;
        const sourceChain = new Set(resolveClassChain(sourceClass, this.reflection));
        const matchesDirection = assocSourceClass != undefined && sourceChain.has(assocSourceClass);

        return { association, matchesDirection };
    }

    /**
     * Determines whether an association end is on the source side of an association
     * relative to a given class.
     *
     * @param associationend The association end to check
     * @param association The containing association
     * @param classType The class whose side membership is being checked
     * @returns True when the end is the source end and the class is in the source chain
     */
    private associationendMatchesAssociationSource(
        associationend: AssociationEndType,
        association: AssociationType,
        classType: ClassType
    ): boolean {
        const sourceEnd = association.source;
        if (sourceEnd?.name === associationend.name) {
            const sourceClass = sourceEnd.class?.ref;
            if (sourceClass) {
                const classChain = new Set(resolveClassChain(classType, this.reflection));
                return classChain.has(sourceClass);
            }
        }
        return false;
    }
}
