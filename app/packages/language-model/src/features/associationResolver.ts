import type { AstReflection } from "@mdeo/language-common";
import {
    Association,
    MetaModel,
    resolveClassChain,
    type AssociationType,
    type ClassType,
    type AssociationEndType
} from "@mdeo/language-metamodel";
import type { AstNode } from "langium";

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
 * This class provides methods to:
 * - Find all associations between two classes
 * - Find a unique association between two classes
 * - Resolve associationend names for both ends of a link
 */
export class AssociationResolver {
    /**
     * Creates a new AssociationResolver.
     *
     * @param reflection The AST reflection for type checking
     */
    constructor(private readonly reflection: AstReflection) {}

    /**
     * Finds all associations between two classes, considering inheritance.
     *
     * @param class1 The first class
     * @param class2 The second class
     * @returns Array of associations that connect the two classes
     */
    findAssociationsBetweenClasses(class1: ClassType, class2: ClassType): AssociationType[] {
        const result: AssociationType[] = [];
        const class1Chain = resolveClassChain(class1, this.reflection);
        const class2Chain = resolveClassChain(class2, this.reflection);

        const metamodels = this.collectMetamodels([...class1Chain, ...class2Chain]);
        const class1ChainSet = new Set(class1Chain);
        const class2ChainSet = new Set(class2Chain);

        for (const metaModel of metamodels) {
            for (const element of metaModel.elements ?? []) {
                if (!this.reflection.isInstance(element, Association)) {
                    continue;
                }

                const assoc = element;
                const sourceClass = assoc.source?.class?.ref;
                const targetClass = assoc.target?.class?.ref;

                if (!sourceClass || !targetClass) {
                    continue;
                }

                const connects = this.associationConnectsClasses(
                    sourceClass,
                    targetClass,
                    class1ChainSet,
                    class2ChainSet
                );

                if (connects) {
                    result.push(assoc);
                }
            }
        }

        return result;
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
     * Finds an association by a associationend name on one end.
     *
     * @param associationend The associationend that is an association end
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
     * Resolves an association and returns both end associationend names.
     *
     * This method handles three cases:
     * 1. Both associationend names are given: validates they belong to the same association
     * 2. One associationend name is given: finds the association and returns both names
     * 3. No associationend names are given: finds the unique association and returns both names
     *
     * @param sourceClass The source class of the link
     * @param targetClass The target class of the link
     * @param sourceAssociationEnd The associationend on the source end, if specified
     * @param targetAssociationEnd The associationend on the target end, if specified
     * @returns The resolved association with both associationend names, or undefined
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
     * Resolves when both properties are specified.
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

        return {
            association: sourceAssoc,
            matchesDirection
        };
    }

    /**
     * Resolves when only one associationend is specified.
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

        if (isSourceAssociationEnd) {
            return {
                association,
                matchesDirection
            };
        } else {
            return {
                association,
                matchesDirection
            };
        }
    }

    /**
     * Resolves when no properties are specified.
     */
    private resolveWithNoProperties(sourceClass: ClassType, targetClass: ClassType): ResolvedAssociation | undefined {
        const association = this.findUniqueAssociation(sourceClass, targetClass);
        if (!association) {
            return undefined;
        }

        const assocSourceClass = association.source?.class?.ref;
        const sourceChain = new Set(resolveClassChain(sourceClass, this.reflection));
        const matchesDirection = assocSourceClass != undefined && sourceChain.has(assocSourceClass);

        if (matchesDirection) {
            return {
                association,
                matchesDirection: true
            };
        } else {
            return {
                association,
                matchesDirection: false
            };
        }
    }

    /**
     * Checks if a associationend is on the source side of an association relative to a class.
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

    /**
     * Collects metamodels from a list of classes.
     */
    private collectMetamodels(classes: ClassType[]): Set<{ elements?: AstNode[] }> {
        const metamodels = new Set<{ elements?: AstNode[] }>();
        for (const cls of classes) {
            const metaModel = this.getMetaModel(cls);
            if (metaModel) {
                metamodels.add(metaModel);
            }
        }
        return metamodels;
    }

    /**
     * Checks if an association connects two class chains.
     */
    private associationConnectsClasses(
        assocSourceClass: ClassType,
        assocTargetClass: ClassType,
        class1ChainSet: Set<ClassType>,
        class2ChainSet: Set<ClassType>
    ): boolean {
        const sourceInClass1 = class1ChainSet.has(assocSourceClass);
        const sourceInClass2 = class2ChainSet.has(assocSourceClass);
        const targetInClass1 = class1ChainSet.has(assocTargetClass);
        const targetInClass2 = class2ChainSet.has(assocTargetClass);

        return (sourceInClass1 && targetInClass2) || (sourceInClass2 && targetInClass1);
    }

    /**
     * Gets the MetaModel containing a class.
     */
    private getMetaModel(classType: ClassType): { elements?: AstNode[] } | undefined {
        let current: AstNode | undefined = classType;
        while (current != undefined) {
            if (this.reflection.isInstance(current, MetaModel)) {
                return current as { elements?: AstNode[] };
            }
            current = current.$container;
        }
        return undefined;
    }
}
