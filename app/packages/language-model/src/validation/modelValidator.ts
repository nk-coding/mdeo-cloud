import type { ValidationAcceptor, ValidationChecks, AstNode } from "langium";
import type { ExtendedLangiumServices } from "@mdeo/language-common";
import {
    EnumTypeReference,
    PrimitiveType,
    Property,
    RangeMultiplicity,
    SingleMultiplicity,
    MetamodelPrimitiveTypes,
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
    type ListValueType
} from "../grammar/modelTypes.js";
import { BaseModelValidator } from "./baseModelValidator.js";
import { sharedImport } from "@mdeo/language-shared";

const { MultiMap } = sharedImport("langium");

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
export class ModelValidator extends BaseModelValidator {
    constructor(private readonly services: ExtendedLangiumServices) {
        super(services.shared.AstReflection);
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
    protected override isRequiredProperty(property: PropertyType): boolean {
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
     * Validates a property assignment, including:
     * - Value type matches property type
     * - Multiplicity is respected (correct number of values)
     */
    validatePropertyAssignment(propAssign: PropertyAssignmentType, accept: ValidationAcceptor): void {
        const propRef = propAssign.name?.ref;
        if (!propRef) {
            return;
        }
        const property = propRef as PropertyType;
        if (!property.type) {
            return;
        }

        const value = propAssign.value;
        if (!value) {
            return;
        }

        this.validateValueMultiplicity(value, property, propAssign, accept);

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
                accept(
                    "error",
                    `Property '${property.name}' expects a single value, but ${count} values were provided.`,
                    {
                        node: propAssign,
                        property: "value"
                    }
                );
                return;
            }

            const bounds = this.getMultiplicityBounds(multiplicity);
            if (count < bounds.lower) {
                accept(
                    "error",
                    `Property '${property.name}' requires at least ${bounds.lower} value(s), but ${count} were provided.`,
                    {
                        node: propAssign,
                        property: "value"
                    }
                );
            }
            if (bounds.upper !== undefined && count > bounds.upper) {
                accept(
                    "error",
                    `Property '${property.name}' allows at most ${bounds.upper} value(s), but ${count} were provided.`,
                    {
                        node: propAssign,
                        property: "value"
                    }
                );
            }
        } else {
            if (isMultiple) {
                const bounds = this.getMultiplicityBounds(multiplicity);
                if (bounds.lower > 1) {
                    accept(
                        "error",
                        `Property '${property.name}' requires at least ${bounds.lower} values. Use list syntax: [value1, value2, ...].`,
                        {
                            node: propAssign,
                            property: "value"
                        }
                    );
                }
            }
        }
    }

    /**
     * Gets the lower and upper bounds of a multiplicity.
     */
    protected override getMultiplicityBounds(multiplicity: MultiplicityType | undefined): {
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
     */
    protected override isMultipleMultiplicity(multiplicity: MultiplicityType | undefined): boolean {
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

        this.validateLinkBase(link, sourceObj, targetObj, accept);
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
            return;
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
}
