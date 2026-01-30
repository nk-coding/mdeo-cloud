import { BaseLabelEditValidator, parseIdentifier, sharedImport, type GModelIndex } from "@mdeo/language-shared";
import type { GModelElement, ValidationStatus as ValidationStatusType } from "@eclipse-glsp/server";
import { MetamodelElementType } from "./model/elementTypes.js";
import type { GClassNode } from "./model/classNode.js";
import type { GEnumNode } from "./model/enumNode.js";
import type { GClassLabel } from "./model/classLabel.js";
import type { GEnumLabel } from "./model/enumLabel.js";
import type { GEnumEntryLabel } from "./model/enumEntryLabel.js";
import type { GPropertyLabel } from "./model/propertyLabel.js";
import {
    Class,
    Enum,
    EnumEntry,
    Property,
    type PropertyType,
    type ClassType,
    type EnumType,
    MetamodelPrimitiveTypes
} from "../../grammar/metamodelTypes.js";
import { resolveClassChain } from "../../features/semanticInformation.js";
import { collectAllPropertyNames } from "../../validation/metamodelValidator.js";

const { injectable, inject } = sharedImport("inversify");
const { ValidationStatus, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Validator for label edits in the metamodel diagram.
 */
@injectable()
export class MetamodelLabelEditValidator extends BaseLabelEditValidator {
    /**
     * Injected model index for accessing AST nodes
     */
    @inject(GModelIndexKey) protected readonly index!: GModelIndex;

    override validate(label: string, element: GModelElement): ValidationStatusType {
        if (element.type === MetamodelElementType.LABEL_CLASS_NAME) {
            return (
                this.validateIdentifier(label, "Class name") ??
                this.validateElementName(label, element as GClassLabel) ??
                ValidationStatus.NONE
            );
        }
        if (element.type === MetamodelElementType.LABEL_ENUM_NAME) {
            return (
                this.validateIdentifier(label, "Enum name") ??
                this.validateElementName(label, element as GEnumLabel) ??
                ValidationStatus.NONE
            );
        }
        if (element.type === MetamodelElementType.LABEL_ENUM_ENTRY) {
            return (
                this.validateIdentifier(label, "Enum entry") ??
                this.validateEnumEntry(label, element as GEnumEntryLabel) ??
                ValidationStatus.NONE
            );
        }
        if (element.type === MetamodelElementType.LABEL_PROPERTY) {
            return this.validatePropertyLabel(label, element as GPropertyLabel) ?? ValidationStatus.NONE;
        }
        if (element.type === MetamodelElementType.LABEL_ASSOCIATION_PROPERTY) {
            return this.validateIdentifier(label, "Property name") ?? ValidationStatus.NONE;
        }
        if (element.type === MetamodelElementType.LABEL_ASSOCIATION_MULTIPLICITY) {
            return this.validateMultiplicity(label) ?? ValidationStatus.NONE;
        }
        return ValidationStatus.NONE;
    }

    /**
     * Validates that the given element name (class or enum) is unique within the model.
     * Both classes and enums share the same namespace.
     *
     * @param name the element name to validate
     * @param element the label element being edited
     * @returns a validation status if the name is not unique, undefined otherwise
     */
    private validateElementName(name: string, element: GClassLabel | GEnumLabel): ValidationStatusType | undefined {
        const trimmedName = name.trim();
        if (element.text === trimmedName) {
            return undefined;
        }
        const conflictingClass = this.modelState.root.children.some(
            (el) => el.type === MetamodelElementType.NODE_CLASS && (el as GClassNode).name === trimmedName
        );
        const conflictingEnum = this.modelState.root.children.some(
            (el) => el.type === MetamodelElementType.NODE_ENUM && (el as GEnumNode).name === trimmedName
        );

        if (conflictingClass) {
            return this.error(`A class with the name '${trimmedName}' already exists.`);
        }
        if (conflictingEnum) {
            return this.error(`An enum with the name '${trimmedName}' already exists.`);
        }
        return undefined;
    }

    /**
     * Validates that the given enum entry name is unique within the enum.
     *
     * @param name the entry name to validate
     * @param element the enum entry label element
     * @returns a validation status if the entry is not unique, undefined otherwise
     */
    private validateEnumEntry(name: string, element: GEnumEntryLabel): ValidationStatusType | undefined {
        const trimmedName = name.trim();
        if (element.text === trimmedName) {
            return undefined;
        }

        const entryNode = this.index.getAstNode(element);
        if (entryNode == undefined) {
            return undefined;
        }

        const reflection = this.modelState.languageServices.shared.AstReflection;
        if (!reflection.isInstance(entryNode, EnumEntry)) {
            return undefined;
        }

        const parentEnum = entryNode.$container;
        if (parentEnum == undefined || !reflection.isInstance(parentEnum, Enum)) {
            return undefined;
        }

        const enumType = parentEnum as EnumType;
        for (const entry of enumType.entries ?? []) {
            if (entry.name === trimmedName) {
                return this.error(`An entry with the name '${trimmedName}' already exists in this enum.`);
            }
        }

        return undefined;
    }

    /**
     * Validates that the given property label follows the correct format and is unique.
     *
     * @param label the label text to validate
     * @param element the property label element
     * @returns a validation status if the label is invalid, undefined otherwise
     */
    private validatePropertyLabel(label: string, element: GPropertyLabel): ValidationStatusType | undefined {
        const formatValidation = this.validatePropertyFormat(label);
        if (formatValidation != undefined) {
            return formatValidation;
        }

        const parsed = parsePropertyLabel(label);
        if (parsed == undefined) {
            return this.error(
                "Invalid property format. Expected: 'name: type' or 'name: type[multiplicity]'. Valid multiplicity formats: [*], [+], [?], [number], [number..*], [number..number]"
            );
        }

        const identifierValidation = this.validateRawIdentifier(parsed.identifier, "Property identifier");
        if (identifierValidation != undefined) {
            return identifierValidation;
        }

        const typeValidation = this.validatePropertyType(parsed.type);
        if (typeValidation != undefined) {
            return typeValidation;
        }

        if (parsed.multiplicity != undefined) {
            const multiplicityValidation = this.validateMultiplicity(parsed.multiplicity);
            if (multiplicityValidation != undefined) {
                return multiplicityValidation;
            }
        }

        const uniquenessValidation = this.validatePropertyUniqueness(parsed.identifier, element);
        if (uniquenessValidation != undefined) {
            return uniquenessValidation;
        }

        return undefined;
    }

    /**
     * Validates that the property label follows the basic format.
     *
     * @param label the label text to validate
     * @returns a validation status if the format is invalid, undefined otherwise
     */
    private validatePropertyFormat(label: string): ValidationStatusType | undefined {
        if (label.trim().length === 0) {
            return this.error("Property label cannot be empty.");
        }
        if (!label.includes(":")) {
            return this.error("Property label must contain a colon (:) separating name and type.");
        }
        return undefined;
    }

    /**
     * Validates that the property type is one of the valid primitive types.
     *
     * @param type the type to validate
     * @returns a validation status if the type is invalid, undefined otherwise
     */
    private validatePropertyType(type: string): ValidationStatusType | undefined {
        const primitiveTypes: string[] = [...Object.values(MetamodelPrimitiveTypes)];

        if (primitiveTypes.includes(type)) {
            return undefined;
        }

        const isEnumType = this.modelState.root.children.some(
            (el) => el.type === MetamodelElementType.NODE_ENUM && (el as GEnumNode).name === type
        );

        if (isEnumType) {
            return undefined;
        }

        return this.error(
            `Invalid property type '${type}'. Valid types are: ${primitiveTypes.join(", ")}, or an enum name.`
        );
    }

    /**
     * Validates the multiplicity format using regex.
     *
     * Valid formats:
     * - Single: *, +, ?, or a number
     * - Range: number..*, or number..number
     *
     * @param multiplicity the multiplicity content to validate (without brackets)
     * @returns a validation status if the multiplicity is invalid, undefined otherwise
     */
    private validateMultiplicity(multiplicity: string): ValidationStatusType | undefined {
        const multiplicityRegex = /^(\*|\+|\?|\d+|\d+\.\.\*|\d+\.\.\d+)$/;

        if (!multiplicityRegex.test(multiplicity)) {
            return this.error(
                `Invalid multiplicity format '${multiplicity}'. Valid formats: *, +, ?, number, number..*, number..number`
            );
        }

        return undefined;
    }

    /**
     * Validates that the property name is unique within the class and its superclass chain.
     * Also considers properties from association ends.
     *
     * @param identifier the property identifier to validate
     * @param element the property label element
     * @returns a validation status if the property name is not unique, undefined otherwise
     */
    private validatePropertyUniqueness(identifier: string, element: GPropertyLabel): ValidationStatusType | undefined {
        const parsedIdentifier = parseIdentifier(identifier);

        const propertyNode = this.index.getAstNode(element);
        if (propertyNode == undefined) {
            return undefined;
        }

        const reflection = this.modelState.languageServices.shared.AstReflection;

        if (!reflection.isInstance(propertyNode, Property)) {
            return undefined;
        }

        const originalProperty = propertyNode as PropertyType;
        const originalParsedName = originalProperty.name;

        // If the name hasn't changed, no need to validate
        if (parsedIdentifier === originalParsedName) {
            return undefined;
        }

        const parentClass = originalProperty.$container;
        if (parentClass == undefined || !reflection.isInstance(parentClass, Class)) {
            return undefined;
        }

        // Use the shared utility function to collect all property names
        const allPropertyNames = collectAllPropertyNames(parentClass as ClassType, reflection);

        // Check if the new name already exists (excluding the original name)
        for (const name of allPropertyNames) {
            if (name === parsedIdentifier && name !== originalParsedName) {
                return this.error(
                    `A property with the name '${parsedIdentifier}' already exists in the class hierarchy.`
                );
            }
        }

        return undefined;
    }
}

/**
 * Parsed property label information.
 */
export interface ParsedPropertyLabel {
    /**
     * The property identifier/name
     */
    identifier: string;
    /**
     * The property type
     */
    type: string;
    /**
     * The multiplicity string content if present (without brackets)
     */
    multiplicity?: string;
}

/**
 * Parses a property label into its components.
 *
 * Expected format: 'name: type' or 'name: type[multiplicity]'
 *
 * @param label the label text to parse
 * @returns the parsed components or undefined if parsing fails
 */
export function parsePropertyLabel(label: string): ParsedPropertyLabel | undefined {
    const colonIndex = label.lastIndexOf(":");
    if (colonIndex < 0) {
        return undefined;
    }

    const identifier = label.substring(0, colonIndex).trim();
    const typeWithMultiplicity = label.substring(colonIndex + 1).trim();

    if (identifier.length === 0 || typeWithMultiplicity.length === 0) {
        return undefined;
    }

    const bracketIndex = typeWithMultiplicity.indexOf("[");
    let multiplicity: string | undefined;
    let type: string;

    if (bracketIndex >= 0) {
        type = typeWithMultiplicity.substring(0, bracketIndex).trim();
        const multiplicityPart = typeWithMultiplicity.substring(bracketIndex);

        if (!multiplicityPart.startsWith("[") || !multiplicityPart.endsWith("]")) {
            return undefined;
        }

        const content = multiplicityPart.substring(1, multiplicityPart.length - 1).trim();
        if (content.length === 0) {
            return undefined;
        }

        multiplicity = content;
    } else {
        type = typeWithMultiplicity;
    }

    if (type.length === 0) {
        return undefined;
    }

    return { identifier, type, multiplicity };
}
