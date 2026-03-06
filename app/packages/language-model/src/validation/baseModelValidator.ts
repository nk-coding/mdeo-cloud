import type { ValidationAcceptor, AstNode } from "langium";
import type { AstReflection } from "@mdeo/language-common";
import {
    Association,
    Enum,
    RangeMultiplicity,
    SingleMultiplicity,
    resolveClassChain,
    type AssociationType,
    type ClassType,
    type EnumType,
    type MultiplicityType,
    type PropertyType
} from "@mdeo/language-metamodel";
import { BaseMetamodelHelper } from "../features/baseMetamodelHelper.js";

/**
 * Base link end interface that both model and transformation link ends must implement.
 */
export interface BaseLinkEnd {
    object?: { ref?: AstNode };
    property?: { ref?: AstNode };
}

/**
 * Base link interface that both model and transformation links must implement.
 */
export interface BaseLink extends AstNode {
    source?: BaseLinkEnd;
    target?: BaseLinkEnd;
}

/**
 * Base object instance interface for objects with classes.
 */
export interface BaseObjectInstance {
    class?: { ref?: AstNode };
}

/**
 * Abstract base validator providing shared validation logic for model-based languages.
 *
 * Extends {@link BaseMetamodelHelper} for shared metamodel traversal and adds
 * validation-specific helpers for associations, multiplicities, and properties.
 */
export abstract class BaseModelValidator extends BaseMetamodelHelper {
    /**
     * Creates a new validator.
     *
     * @param reflection The AST reflection used for runtime type checks
     */
    constructor(reflection: AstReflection) {
        super(reflection);
    }

    /**
     * Validates a link between two object instances.
     * Checks that appropriate associations exist between the classes.
     *
     * @param link The link to validate
     * @param sourceObj The source object instance
     * @param targetObj The target object instance
     * @param accept The validation acceptor
     */
    protected validateLinkBase(
        link: BaseLink,
        sourceObj: BaseObjectInstance,
        targetObj: BaseObjectInstance,
        accept: ValidationAcceptor
    ): void {
        const source = link.source;
        const target = link.target;

        if (!source || !target) {
            return;
        }

        const sourceClass = this.resolveToClass(sourceObj.class?.ref);
        const targetClass = this.resolveToClass(targetObj.class?.ref);

        if (!sourceClass || !targetClass) {
            return;
        }

        const sourceHasProperty = !!source.property?.ref;
        const targetHasProperty = !!target.property?.ref;

        if (!sourceHasProperty && !targetHasProperty) {
            this.validateUniqueAssociation(link, sourceClass, targetClass, accept);
        } else if (sourceHasProperty && targetHasProperty) {
            this.validateSameAssociation(link, source, target, accept);
        } else {
            this.validateSinglePropertyLink(link, source, target, sourceClass, targetClass, accept);
        }
    }

    /**
     * Validates that there is exactly one association between two classes when no properties are specified.
     *
     * @param link The link being validated
     * @param sourceClass The source class type
     * @param targetClass The target class type
     * @param accept The validation acceptor
     */
    protected validateUniqueAssociation(
        link: AstNode,
        sourceClass: ClassType,
        targetClass: ClassType,
        accept: ValidationAcceptor
    ): void {
        const associations = this.findAssociationsBetweenClasses(sourceClass, targetClass);

        if (associations.length === 0) {
            accept("error", `No association exists between '${sourceClass.name}' and '${targetClass.name}'.`, {
                node: link
            });
        } else if (associations.length > 1) {
            accept(
                "error",
                `Multiple associations exist between '${sourceClass.name}' and '${targetClass.name}'. Please specify which association to use by adding property names.`,
                { node: link }
            );
        }
    }

    /**
     * Validates that both ends of a link reference properties from the same association.
     *
     * @param link The link being validated
     * @param source The source link end
     * @param target The target link end
     * @param accept The validation acceptor
     */
    protected validateSameAssociation(
        link: AstNode,
        source: BaseLinkEnd,
        target: BaseLinkEnd,
        accept: ValidationAcceptor
    ): void {
        const sourceProperty = source.property?.ref as PropertyType | undefined;
        const targetProperty = target.property?.ref as PropertyType | undefined;

        if (!sourceProperty || !targetProperty) {
            return;
        }

        const sourceAssociation = this.findAssociationForProperty(sourceProperty);
        const targetAssociation = this.findAssociationForProperty(targetProperty);

        if (!sourceAssociation || !targetAssociation) {
            accept("error", `Link properties must be association end properties, not regular class properties.`, {
                node: link
            });
            return;
        }

        if (sourceAssociation !== targetAssociation) {
            accept("error", `Source and target properties must be from the same association.`, { node: link });
            return;
        }

        if (sourceProperty === targetProperty) {
            accept(
                "error",
                `Source and target properties must reference opposite association ends, not the same end '${sourceProperty.name}'.`,
                { node: link }
            );
        }
    }

    /**
     * Validates a link where only one end has a property specified.
     * Verifies that the property is a valid association end connecting the classes.
     *
     * @param link The link being validated
     * @param source The source link end
     * @param target The target link end
     * @param sourceClass The source class type
     * @param targetClass The target class type
     * @param accept The validation acceptor
     */
    protected validateSinglePropertyLink(
        link: AstNode,
        source: BaseLinkEnd,
        target: BaseLinkEnd,
        sourceClass: ClassType,
        targetClass: ClassType,
        accept: ValidationAcceptor
    ): void {
        const sourceProperty = source.property?.ref as PropertyType | undefined;
        const targetProperty = target.property?.ref as PropertyType | undefined;
        const property = sourceProperty ?? targetProperty;
        const isSourceProperty = sourceProperty !== undefined;

        if (property == undefined) {
            return;
        }

        const association = this.findAssociationForProperty(property);
        if (!association) {
            accept("error", `Property '${property.name}' is not an association end property.`, {
                node: link,
                property: isSourceProperty ? "source" : "target"
            });
            return;
        }

        const assocSourceClass = this.resolveToClass(association.source?.class?.ref);
        const assocTargetClass = this.resolveToClass(association.target?.class?.ref);

        if (!assocSourceClass || !assocTargetClass) {
            return;
        }

        const sourceChain = new Set(resolveClassChain(sourceClass, this.reflection));
        const targetChain = new Set(resolveClassChain(targetClass, this.reflection));

        const sourceMatches = sourceChain.has(assocSourceClass);
        const targetMatches = targetChain.has(assocTargetClass);
        const reverseSourceMatches = sourceChain.has(assocTargetClass);
        const reverseTargetMatches = targetChain.has(assocSourceClass);

        const connectsCorrectly = (sourceMatches && targetMatches) || (reverseSourceMatches && reverseTargetMatches);

        if (!connectsCorrectly) {
            accept(
                "error",
                `Property '${property.name}' belongs to an association that does not connect '${sourceClass.name}' and '${targetClass.name}'.`,
                { node: link }
            );
        }
    }

    /**
     * Finds the association that contains a given property as an association end.
     *
     * @param property The property to find the containing association for
     * @returns The containing association, or undefined if not found
     */
    protected findAssociationForProperty(property: PropertyType): AssociationType | undefined {
        const container = property.$container;

        if (container && this.reflection.isInstance(container, Association)) {
            return container;
        }

        return undefined;
    }

    /**
     * Checks if a property is required (multiplicity is not ? or 0..1).
     *
     * @param property The property to check
     * @returns True if the property is required
     */
    protected isRequiredProperty(property: PropertyType): boolean {
        const multiplicity = property.multiplicity;

        if (multiplicity == undefined) {
            return true;
        }

        if (this.reflection.isInstance(multiplicity, SingleMultiplicity)) {
            const value = multiplicity.value;
            const numericValue = multiplicity.numericValue;

            if (value === "?") {
                return false;
            }

            if (numericValue === 0) {
                return false;
            }

            return true;
        }

        if (this.reflection.isInstance(multiplicity, RangeMultiplicity)) {
            const lower = multiplicity.lower;

            return lower > 0;
        }

        return true;
    }

    /**
     * Gets the lower and upper bounds of a multiplicity.
     *
     * @param multiplicity The multiplicity to get bounds for
     * @returns Object with lower and upper bounds
     */
    protected getMultiplicityBounds(multiplicity: MultiplicityType | undefined): {
        lower: number;
        upper: number | undefined;
    } {
        if (!multiplicity) {
            return { lower: 1, upper: 1 };
        }

        if (this.reflection.isInstance(multiplicity, SingleMultiplicity)) {
            const value = multiplicity.value;
            const numericValue = multiplicity.numericValue;

            if (value === "?") {
                return { lower: 0, upper: 1 };
            }
            if (value === "*") {
                return { lower: 0, upper: undefined };
            }
            if (value === "+") {
                return { lower: 1, upper: undefined };
            }
            if (numericValue !== undefined) {
                return { lower: numericValue, upper: numericValue };
            }
        }

        if (this.reflection.isInstance(multiplicity, RangeMultiplicity)) {
            const lower = multiplicity.lower;
            const upper = multiplicity.upper === "*" ? undefined : multiplicity.upperNumeric;
            return { lower, upper };
        }

        return { lower: 1, upper: 1 };
    }

    /**
     * Checks if a multiplicity allows multiple values.
     *
     * @param multiplicity The multiplicity to check
     * @returns True if the multiplicity allows multiple values
     */
    protected isMultipleMultiplicity(multiplicity: MultiplicityType | undefined): boolean {
        if (!multiplicity) {
            return false;
        }

        if (this.reflection.isInstance(multiplicity, SingleMultiplicity)) {
            const value = multiplicity.value;
            const numericValue = multiplicity.numericValue;

            if (value === "*" || value === "+") {
                return true;
            }
            if (numericValue !== undefined && numericValue > 1) {
                return true;
            }
            return false;
        }

        if (this.reflection.isInstance(multiplicity, RangeMultiplicity)) {
            const upper = multiplicity.upper;
            const upperNumeric = multiplicity.upperNumeric;

            if (upper === "*" || (upperNumeric !== undefined && upperNumeric > 1)) {
                return true;
            }
            return false;
        }

        return false;
    }

    /**
     * Resolves an AstNode to its actual Enum.
     *
     * @param enumNode The enum node to resolve
     * @returns The resolved enum type, or undefined if not an Enum
     */
    protected resolveToEnum(enumNode: AstNode | undefined): EnumType | undefined {
        if (enumNode == undefined) {
            return undefined;
        }
        if (this.reflection.isInstance(enumNode, Enum)) {
            return enumNode;
        }
        return undefined;
    }
}
