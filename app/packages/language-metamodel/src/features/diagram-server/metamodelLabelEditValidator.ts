import { BaseLabelEditValidator, parseIdentifier, sharedImport, type GModelIndex } from "@mdeo/language-shared";
import type { GModelElement, ValidationStatus as ValidationStatusType } from "@eclipse-glsp/server";
import { MetamodelElementType } from "./model/elementTypes.js";
import type { GClassNode } from "./model/classNode.js";
import type { GClassLabel } from "./model/classLabel.js";
import type { GPropertyLabel } from "./model/propertyLabel.js";
import { Class, type PropertyType, type ClassType, MetamodelPrimitiveTypes } from "../../grammar/metamodelTypes.js";
import { resolveClassChain } from "../../features/semanticInformation.js";

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
                this.validateClassName(label, element as GClassLabel) ??
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
     * Validates that the given class name is unique within the model.
     *
     * @param name the class name to validate
     * @returns a validation status if the name is not unique, undefined otherwise
     */
    private validateClassName(name: string, element: GClassLabel): ValidationStatusType | undefined {
        const trimmedName = name.trim();
        if (element.text === trimmedName) {
            return undefined;
        }
        if (
            this.modelState.root.children.some(
                (element) =>
                    element.type === MetamodelElementType.NODE_CLASS && (element as GClassNode).name === trimmedName
            )
        ) {
            return this.error(`A class with the name '${trimmedName}' already exists.`);
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
        const validTypes: string[] = [...Object.values(MetamodelPrimitiveTypes)];

        if (!validTypes.includes(type)) {
            return this.error(`Invalid property type '${type}'. Valid types are: ${validTypes.join(", ")}.`);
        }

        return undefined;
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

        if (!reflection.isInstance(propertyNode, Class)) {
            const originalProperty = propertyNode as PropertyType;
            const originalParsedName = originalProperty.name;

            if (parsedIdentifier === originalParsedName) {
                return undefined;
            }

            const parentClass = originalProperty.$container;
            if (parentClass == undefined || !reflection.isInstance(parentClass, Class)) {
                return undefined;
            }

            const classChain = resolveClassChain(parentClass as ClassType, reflection);

            for (const cls of classChain) {
                for (const prop of cls.properties ?? []) {
                    if (prop.name === parsedIdentifier) {
                        return this.error(
                            `A property with the name '${parsedIdentifier}' already exists in the class hierarchy.`
                        );
                    }
                }
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

        // Check if brackets are matched and have content
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
