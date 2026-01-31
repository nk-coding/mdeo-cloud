/**
 * @module associationResolver
 *
 * Provides utilities for resolving associations between classes in metamodels.
 * Used by validators to check link validity and by TypedAst converters to
 * extract association property names.
 */
import type { AstReflection } from "@mdeo/language-common";
import {
    Association,
    Class,
    MetaModel,
    resolveClassChain,
    type AssociationType,
    type ClassType,
    type PropertyType
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
     * The property name on the source side of the link.
     */
    sourcePropertyName?: string;

    /**
     * The property name on the target side of the link.
     */
    targetPropertyName?: string;

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
 * - Resolve property names for both ends of a link
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
                const sourceClass = this.resolveToClass(assoc.source?.class?.ref);
                const targetClass = this.resolveToClass(assoc.target?.class?.ref);

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
     * Finds an association by a property name on one end.
     *
     * @param property The property that is an association end
     * @returns The containing association, or undefined if not found
     */
    findAssociationForProperty(property: PropertyType): AssociationType | undefined {
        const container = property.$container;
        if (container && this.reflection.isInstance(container, Association)) {
            return container;
        }
        return undefined;
    }

    /**
     * Resolves an association and returns both end property names.
     *
     * This method handles three cases:
     * 1. Both property names are given: validates they belong to the same association
     * 2. One property name is given: finds the association and returns both names
     * 3. No property names are given: finds the unique association and returns both names
     *
     * @param sourceClass The source class of the link
     * @param targetClass The target class of the link
     * @param sourceProperty The property on the source end, if specified
     * @param targetProperty The property on the target end, if specified
     * @returns The resolved association with both property names, or undefined
     */
    resolveAssociation(
        sourceClass: ClassType,
        targetClass: ClassType,
        sourceProperty?: PropertyType,
        targetProperty?: PropertyType
    ): ResolvedAssociation | undefined {
        if (sourceProperty != undefined && targetProperty != undefined) {
            return this.resolveWithBothProperties(sourceProperty, targetProperty, sourceClass);
        }

        if (sourceProperty != undefined || targetProperty != undefined) {
            return this.resolveWithOneProperty(
                sourceClass,
                targetClass,
                sourceProperty ?? targetProperty!,
                sourceProperty != undefined
            );
        }

        return this.resolveWithNoProperties(sourceClass, targetClass);
    }

    /**
     * Resolves when both properties are specified.
     */
    private resolveWithBothProperties(
        sourceProperty: PropertyType,
        targetProperty: PropertyType,
        sourceClass: ClassType
    ): ResolvedAssociation | undefined {
        const sourceAssoc = this.findAssociationForProperty(sourceProperty);
        const targetAssoc = this.findAssociationForProperty(targetProperty);

        if (!sourceAssoc || !targetAssoc || sourceAssoc !== targetAssoc) {
            return undefined;
        }

        const matchesDirection = this.propertyMatchesAssociationSource(sourceProperty, sourceAssoc, sourceClass);

        return {
            association: sourceAssoc,
            sourcePropertyName: sourceProperty.name,
            targetPropertyName: targetProperty.name,
            matchesDirection
        };
    }

    /**
     * Resolves when only one property is specified.
     */
    private resolveWithOneProperty(
        sourceClass: ClassType,
        targetClass: ClassType,
        property: PropertyType,
        isSourceProperty: boolean
    ): ResolvedAssociation | undefined {
        const association = this.findAssociationForProperty(property);
        if (!association) {
            return undefined;
        }

        const sourceEnd = association.source;
        const targetEnd = association.target;

        const matchesDirection = this.propertyMatchesAssociationSource(
            property,
            association,
            isSourceProperty ? sourceClass : targetClass
        );

        if (isSourceProperty) {
            const oppositeEnd = matchesDirection ? targetEnd : sourceEnd;
            return {
                association,
                sourcePropertyName: property.name,
                targetPropertyName: oppositeEnd?.name,
                matchesDirection
            };
        } else {
            const oppositeEnd = matchesDirection ? sourceEnd : targetEnd;
            return {
                association,
                sourcePropertyName: oppositeEnd?.name,
                targetPropertyName: property.name,
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

        const assocSourceClass = this.resolveToClass(association.source?.class?.ref);
        const sourceChain = new Set(resolveClassChain(sourceClass, this.reflection));
        const matchesDirection = assocSourceClass != undefined && sourceChain.has(assocSourceClass);

        if (matchesDirection) {
            return {
                association,
                sourcePropertyName: association.source?.name,
                targetPropertyName: association.target?.name,
                matchesDirection: true
            };
        } else {
            return {
                association,
                sourcePropertyName: association.target?.name,
                targetPropertyName: association.source?.name,
                matchesDirection: false
            };
        }
    }

    /**
     * Checks if a property is on the source side of an association relative to a class.
     */
    private propertyMatchesAssociationSource(
        property: PropertyType,
        association: AssociationType,
        classType: ClassType
    ): boolean {
        const sourceEnd = association.source;
        if (sourceEnd?.name === property.name) {
            const sourceClass = this.resolveToClass(sourceEnd.class?.ref);
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

    /**
     * Resolves an AstNode to its actual Class.
     */
    private resolveToClass(classNode: AstNode | undefined): ClassType | undefined {
        if (classNode == undefined) {
            return undefined;
        }
        if (this.reflection.isInstance(classNode, Class)) {
            return classNode;
        }
        return undefined;
    }
}
