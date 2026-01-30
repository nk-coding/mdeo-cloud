import type { ValidationAcceptor, ValidationChecks, AstNode } from "langium";
import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import { MultiMap } from "langium";
import {
    Class,
    ClassOrEnumImport,
    Enum,
    EnumTypeReference,
    PrimitiveType,
    Property,
    RangeMultiplicity,
    SingleMultiplicity,
    MetamodelPrimitiveTypes,
    Association,
    MetaModel,
    type AssociationType,
    type ClassType,
    type EnumType,
    type EnumTypeReferenceType,
    type MultiplicityType,
    type PropertyType
} from "@mdeo/language-metamodel";
import { resolveClassChain } from "@mdeo/language-metamodel";
import {
    ListValue,
    SimpleValue,
    EnumValue,
    type LinkType,
    type ModelType,
    type ObjectInstanceType,
    type PropertyAssignmentType,
    type SimpleValueType,
    type EnumValueType,
    type ListValueType,
    type LinkEndType
} from "../grammar/modelTypes.js";

/**
 * Interface mapping for model AST types used in validation checks.
 */
interface ModelAstTypes {
    Model: ModelType;
    ObjectInstance: ObjectInstanceType;
    PropertyAssignment: PropertyAssignmentType;
    Link: LinkType;
}

/**
 * Registers validation checks for the model language.
 *
 * @param services The language services
 */
export function registerModelValidationChecks(services: ExtendedLangiumServices): void {
    const registry = services.validation.ValidationRegistry;
    const validator = new ModelValidator(services);

    const checks: ValidationChecks<ModelAstTypes> = {
        Model: validator.validateModel.bind(validator),
        ObjectInstance: validator.validateObjectInstance.bind(validator),
        PropertyAssignment: validator.validatePropertyAssignment.bind(validator),
        Link: validator.validateLink.bind(validator)
    };

    registry.register(checks, validator);
}

/**
 * Validator for model language constructs.
 */
export class ModelValidator {
    private readonly reflection: AstReflection;

    constructor(private readonly services: ExtendedLangiumServices) {
        this.reflection = services.shared.AstReflection;
    }

    /**
     * Validates the entire model for object name uniqueness.
     */
    validateModel(model: ModelType, accept: ValidationAcceptor): void {
        const nameToObjects = new MultiMap<string, ObjectInstanceType>();

        for (const obj of model.objects ?? []) {
            if (obj.name) {
                nameToObjects.add(obj.name, obj);
            }
        }

        for (const [name, objects] of nameToObjects.entriesGroupedByKey()) {
            if (objects.length > 1) {
                for (const obj of objects) {
                    accept("error", `Duplicate object name: '${name}'.`, {
                        node: obj,
                        property: "name"
                    });
                }
            }
        }
    }

    /**
     * Validates an object instance, including:
     * - Class must not be abstract
     * - All required properties must be defined
     */
    validateObjectInstance(obj: ObjectInstanceType, accept: ValidationAcceptor): void {
        this.validateClassNotAbstract(obj, accept);
        this.validateRequiredProperties(obj, accept);
    }

    /**
     * Validates that the class an object is an instance of is not abstract.
     */
    private validateClassNotAbstract(obj: ObjectInstanceType, accept: ValidationAcceptor): void {
        const classRef = obj.class?.ref;
        if (!classRef) {
            return;
        }

        const classType = this.resolveToClass(classRef);
        if (!classType) {
            return;
        }

        if (classType.isAbstract) {
            accept("error", `Cannot instantiate abstract class '${classType.name}'.`, {
                node: obj,
                property: "class"
            });
        }
    }

    /**
     * Validates that all required properties (multiplicity not ? or 0..1) are defined.
     */
    private validateRequiredProperties(obj: ObjectInstanceType, accept: ValidationAcceptor): void {
        const classRef = obj.class?.ref;
        if (!classRef) {
            return;
        }

        const classType = this.resolveToClass(classRef);
        if (!classType) {
            return;
        }

        const classChain = resolveClassChain(classType, this.reflection);
        const definedPropertyNames = new Set<string>();

        for (const propAssign of obj.properties ?? []) {
            const propRef = propAssign.name?.ref;
            if (propRef && this.reflection.isInstance(propRef, Property)) {
                const prop = propRef as PropertyType;
                if (prop.name) {
                    definedPropertyNames.add(prop.name);
                }
            }
        }

        // Check all properties in the class chain
        for (const cls of classChain) {
            for (const prop of cls.properties ?? []) {
                if (!prop.name) {
                    continue;
                }

                if (!definedPropertyNames.has(prop.name)) {
                    if (this.isRequiredProperty(prop)) {
                        accept(
                            "error",
                            `Required property '${prop.name}' is not defined. Properties with multiplicity other than '?' or '0..1' must be assigned.`,
                            { node: obj, property: "name" }
                        );
                    }
                }
            }
        }
    }

    /**
     * Checks if a property is required (multiplicity is not ? or 0..1).
     */
    private isRequiredProperty(property: PropertyType): boolean {
        const multiplicity = property.multiplicity;

        if (!multiplicity) {
            // No multiplicity means exactly 1, which is required
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
     * Validates a property assignment, including:
     * - Value type matches property type
     * - Multiplicity is respected (correct number of values)
     */
    validatePropertyAssignment(propAssign: PropertyAssignmentType, accept: ValidationAcceptor): void {
        const propRef = propAssign.name?.ref;
        if (!propRef) {
            return;
        }

        // The property reference might be a Property from the metamodel
        // We need to handle this properly
        const property = propRef as PropertyType;
        if (!property.type) {
            return;
        }

        const value = propAssign.value;
        if (!value) {
            return;
        }

        // Validate multiplicity (number of values)
        this.validateValueMultiplicity(value, property, propAssign, accept);

        // Validate value type
        this.validateValueType(value, property, propAssign, accept);
    }

    /**
     * Validates that the number of values matches the property multiplicity.
     */
    private validateValueMultiplicity(
        value: AstNode,
        property: PropertyType,
        propAssign: PropertyAssignmentType,
        accept: ValidationAcceptor
    ): void {
        const multiplicity = property.multiplicity;
        const isMultiple = this.isMultipleMultiplicity(multiplicity);

        if (this.reflection.isInstance(value, ListValue)) {
            const listValue = value as ListValueType;
            const count = listValue.values?.length ?? 0;

            if (!isMultiple && count > 1) {
                accept("error", `Property '${property.name}' expects a single value, but ${count} values were provided.`, {
                    node: propAssign,
                    property: "value"
                });
                return;
            }

            // Check bounds
            const bounds = this.getMultiplicityBounds(multiplicity);
            if (count < bounds.lower) {
                accept("error", `Property '${property.name}' requires at least ${bounds.lower} value(s), but ${count} were provided.`, {
                    node: propAssign,
                    property: "value"
                });
            }
            if (bounds.upper !== undefined && count > bounds.upper) {
                accept("error", `Property '${property.name}' allows at most ${bounds.upper} value(s), but ${count} were provided.`, {
                    node: propAssign,
                    property: "value"
                });
            }
        } else {
            // Single value
            if (isMultiple) {
                const bounds = this.getMultiplicityBounds(multiplicity);
                if (bounds.lower > 1) {
                    accept("error", `Property '${property.name}' requires at least ${bounds.lower} values. Use list syntax: [value1, value2, ...].`, {
                        node: propAssign,
                        property: "value"
                    });
                }
            }
        }
    }

    /**
     * Gets the lower and upper bounds of a multiplicity.
     */
    private getMultiplicityBounds(multiplicity: MultiplicityType | undefined): { lower: number; upper: number | undefined } {
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
     */
    private isMultipleMultiplicity(multiplicity: MultiplicityType | undefined): boolean {
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
     * Validates that the value type matches the property type.
     */
    private validateValueType(
        value: AstNode,
        property: PropertyType,
        propAssign: PropertyAssignmentType,
        accept: ValidationAcceptor
    ): void {
        if (this.reflection.isInstance(value, ListValue)) {
            const listValue = value as ListValueType;
            for (const singleValue of listValue.values ?? []) {
                this.validateSingleValueType(singleValue, property, propAssign, accept);
            }
        } else {
            this.validateSingleValueType(value, property, propAssign, accept);
        }
    }

    /**
     * Validates a single value against the property type.
     */
    private validateSingleValueType(
        value: AstNode,
        property: PropertyType,
        propAssign: PropertyAssignmentType,
        accept: ValidationAcceptor
    ): void {
        const propertyType = property.type;

        if (this.reflection.isInstance(propertyType, PrimitiveType)) {
            this.validatePrimitiveValue(value, propertyType.name, property, propAssign, accept);
        } else if (this.reflection.isInstance(propertyType, EnumTypeReference)) {
            this.validateEnumValue(value, propertyType, property, propAssign, accept);
        }
    }

    /**
     * Validates a value against a primitive type.
     */
    private validatePrimitiveValue(
        value: AstNode,
        primitiveType: MetamodelPrimitiveTypes | undefined,
        property: PropertyType,
        propAssign: PropertyAssignmentType,
        accept: ValidationAcceptor
    ): void {
        if (!this.reflection.isInstance(value, SimpleValue)) {
            accept("error", `Property '${property.name}' expects a primitive value, not an enum value.`, {
                node: propAssign,
                property: "value"
            });
            return;
        }

        const simpleValue = value as SimpleValueType;

        switch (primitiveType) {
            case MetamodelPrimitiveTypes.STRING:
                if (simpleValue.stringValue === undefined) {
                    accept("error", `Property '${property.name}' expects a string value.`, {
                        node: propAssign,
                        property: "value"
                    });
                }
                break;

            case MetamodelPrimitiveTypes.BOOLEAN:
                if (simpleValue.booleanValue === undefined) {
                    accept("error", `Property '${property.name}' expects a boolean value (true or false).`, {
                        node: propAssign,
                        property: "value"
                    });
                }
                break;

            case MetamodelPrimitiveTypes.INT:
            case MetamodelPrimitiveTypes.LONG:
                if (simpleValue.numberValue === undefined) {
                    accept("error", `Property '${property.name}' expects an integer value.`, {
                        node: propAssign,
                        property: "value"
                    });
                } else if (!Number.isInteger(simpleValue.numberValue)) {
                    accept("error", `Property '${property.name}' expects an integer value, not a decimal.`, {
                        node: propAssign,
                        property: "value"
                    });
                }
                break;

            case MetamodelPrimitiveTypes.FLOAT:
            case MetamodelPrimitiveTypes.DOUBLE:
                if (simpleValue.numberValue === undefined) {
                    accept("error", `Property '${property.name}' expects a numeric value.`, {
                        node: propAssign,
                        property: "value"
                    });
                }
                break;
        }
    }

    /**
     * Validates a value against an enum type.
     */
    private validateEnumValue(
        value: AstNode,
        enumTypeRef: EnumTypeReferenceType,
        property: PropertyType,
        propAssign: PropertyAssignmentType,
        accept: ValidationAcceptor
    ): void {
        if (!this.reflection.isInstance(value, EnumValue)) {
            accept("error", `Property '${property.name}' expects an enum value.`, {
                node: propAssign,
                property: "value"
            });
            return;
        }

        const enumValue = value as EnumValueType;
        const enumRef = enumTypeRef.enum?.ref;

        if (!enumRef) {
            return; // Enum type not resolved
        }

        const enumType = this.resolveToEnum(enumRef);
        if (!enumType) {
            return;
        }

        const valueRef = enumValue.value?.ref;
        if (!valueRef) {
            accept("error", `Invalid enum value for property '${property.name}'.`, {
                node: propAssign,
                property: "value"
            });
            return;
        }

        // Check that the enum entry belongs to the correct enum
        const entryParent = valueRef.$container;
        if (entryParent !== enumType) {
            const validEntries = (enumType.entries ?? []).map((e) => e.name).filter((n) => n !== undefined);
            accept(
                "error",
                `Invalid enum value for property '${property.name}'. Expected one of: ${validEntries.join(", ")}.`,
                { node: propAssign, property: "value" }
            );
        }
    }

    /**
     * Validates a link, including:
     * - If neither end has a property: there must be exactly one association between the classes
     * - If both ends have properties: they must be part of the same association
     */
    validateLink(link: LinkType, accept: ValidationAcceptor): void {
        const source = link.source;
        const target = link.target;

        if (!source?.object?.ref || !target?.object?.ref) {
            return;
        }

        const sourceObj = source.object.ref as ObjectInstanceType;
        const targetObj = target.object.ref as ObjectInstanceType;

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
     * Validates a link where only one end has a property specified.
     * Verifies that the property is a valid association end connecting the classes.
     */
    private validateSinglePropertyLink(
        link: LinkType,
        source: LinkEndType,
        target: LinkEndType,
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
            accept(
                "error",
                `Property '${property.name}' is not an association end property.`,
                { node: link, property: isSourceProperty ? "source" : "target" }
            );
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
     * Validates that there is exactly one association between two classes when no properties are specified.
     */
    private validateUniqueAssociation(
        link: LinkType,
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
     */
    private validateSameAssociation(
        link: LinkType,
        source: LinkEndType,
        target: LinkEndType,
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
            accept(
                "error",
                `Link properties must be association end properties, not regular class properties.`,
                { node: link }
            );
            return;
        }

        if (sourceAssociation !== targetAssociation) {
            accept(
                "error",
                `Source and target properties must be from the same association.`,
                { node: link }
            );
        }
    }

    /**
     * Finds all associations between two classes (considering class chains).
     */
    private findAssociationsBetweenClasses(
        class1: ClassType,
        class2: ClassType
    ): AssociationType[] {
        const result: AssociationType[] = [];
        const class1Chain = resolveClassChain(class1, this.reflection);
        const class2Chain = resolveClassChain(class2, this.reflection);

        const metamodels = new Set<{ elements?: AstNode[] }>();
        
        for (const cls of class1Chain) {
            const metaModel = this.getMetaModel(cls);
            if (metaModel) {
                metamodels.add(metaModel);
            }
        }
        
        for (const cls of class2Chain) {
            const metaModel = this.getMetaModel(cls);
            if (metaModel) {
                metamodels.add(metaModel);
            }
        }

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

                const sourceInClass1 = class1ChainSet.has(sourceClass);
                const sourceInClass2 = class2ChainSet.has(sourceClass);
                const targetInClass1 = class1ChainSet.has(targetClass);
                const targetInClass2 = class2ChainSet.has(targetClass);

                if ((sourceInClass1 && targetInClass2) || (sourceInClass2 && targetInClass1)) {
                    result.push(assoc);
                }
            }
        }

        return result;
    }

    /**
     * Finds the association that contains a given property (as an association end).
     */
    private findAssociationForProperty(property: PropertyType): AssociationType | undefined {
        const container = property.$container;

        if (container && this.reflection.isInstance(container, Association)) {
            return container;
        }

        return undefined;
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
     * Resolves a ClassOrImport to its actual Class.
     */
    private resolveToClass(classOrImport: AstNode | undefined): ClassType | undefined {
        if (classOrImport == undefined) {
            return undefined;
        }
        if (this.reflection.isInstance(classOrImport, Class)) {
            return classOrImport;
        }
        if (this.reflection.isInstance(classOrImport, ClassOrEnumImport)) {
            const entity = classOrImport.entity?.ref;
            if (entity && this.reflection.isInstance(entity, Class)) {
                return entity;
            }
        }
        return undefined;
    }

    /**
     * Resolves an EnumOrImport to its actual Enum.
     */
    private resolveToEnum(enumOrImport: AstNode | undefined): EnumType | undefined {
        if (enumOrImport == undefined) {
            return undefined;
        }
        if (this.reflection.isInstance(enumOrImport, Enum)) {
            return enumOrImport;
        }
        if (this.reflection.isInstance(enumOrImport, ClassOrEnumImport)) {
            const entity = enumOrImport.entity?.ref;
            if (entity && this.reflection.isInstance(entity, Enum)) {
                return entity;
            }
        }
        return undefined;
    }
}
