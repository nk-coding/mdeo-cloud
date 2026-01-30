import { BaseLabelEditValidator, parseIdentifier, sharedImport, type GModelIndex } from "@mdeo/language-shared";
import type { GModelElement, ValidationStatus as ValidationStatusType } from "@eclipse-glsp/server";
import { ModelElementType } from "./model/elementTypes.js";
import type { GObjectNode } from "./model/objectNode.js";
import type { GObjectNameLabel } from "./model/objectNameLabel.js";
import type { GPropertyLabel } from "./model/propertyLabel.js";
import {
    resolveClassChain,
    Class,
    Enum,
    EnumTypeReference,
    PrimitiveType,
    RangeMultiplicity,
    SingleMultiplicity,
    MetamodelPrimitiveTypes,
    type PropertyType,
    type ClassType,
    type EnumType,
    type EnumTypeReferenceType,
    type MultiplicityType
} from "@mdeo/language-metamodel";
import { PropertyAssignment, type ObjectInstanceType } from "../../grammar/modelTypes.js";
import type { PartialPropertyAssignment } from "../../grammar/modelPartialTypes.js";
import type { IToken } from "chevrotain";
import { FLOAT, ID, INT, STRING } from "@mdeo/language-common";
import { BOOLEAN } from "../../grammar/modelRules.js";
import type { Lexer } from "langium";
import { isMultipleMultiplicity } from "../../grammar/util.js";

const { injectable, inject } = sharedImport("inversify");
const { ValidationStatus, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Validator for label edits in the model diagram.
 */
@injectable()
export class ModelLabelEditValidator extends BaseLabelEditValidator {
    /**
     * Injected model index for accessing AST nodes.
     */
    @inject(GModelIndexKey)
    protected readonly index!: GModelIndex;

    /**
     * Validates a label edit operation.
     *
     * @param label The new label text
     * @param element The element being edited
     * @returns Validation status
     */
    override validate(label: string, element: GModelElement): ValidationStatusType {
        if (element.type === ModelElementType.LABEL_OBJECT_NAME) {
            return this.validateObjectLabel(label, element as GObjectNameLabel) ?? ValidationStatus.NONE;
        }

        if (element.type === ModelElementType.LABEL_PROPERTY) {
            return this.validatePropertyValueLabel(label, element as GPropertyLabel) ?? ValidationStatus.NONE;
        }

        return ValidationStatus.NONE;
    }

    /**
     * Validates an object label (name : type format).
     *
     * @param label The label text to validate
     * @param element The object label element being edited
     * @returns A validation status if invalid, undefined otherwise
     */
    private validateObjectLabel(label: string, element: GObjectNameLabel): ValidationStatusType | undefined {
        const parsed = this.parseObjectLabel(label);

        if (parsed == undefined) {
            return this.error("Invalid object format. Expected: name : type");
        }

        const nameValidation = this.validateIdentifier(parsed.name, "Object name");
        if (nameValidation != undefined) {
            return nameValidation;
        }

        const typeValidation = this.validateIdentifier(parsed.type, "Object type");
        if (typeValidation != undefined) {
            return typeValidation;
        }

        const trimmedName = parsed.name.trim();
        const currentText = element.text || "";
        const currentParsed = this.parseObjectLabel(currentText);

        if (currentParsed?.name !== trimmedName) {
            const conflictingObject = this.modelState.root.children.some(
                (el) => el.type === ModelElementType.NODE_OBJECT && (el as GObjectNode).name === trimmedName
            );

            if (conflictingObject) {
                return this.error(`An object with the name '${trimmedName}' already exists.`);
            }
        }

        return undefined;
    }

    /**
     * Parses an object label into name and type parts.
     *
     * @param label The label text (format: "name : type")
     * @returns Parsed name and type, or undefined if invalid
     */
    private parseObjectLabel(label: string): { name: string; type: string } | undefined {
        const colonIndex = label.lastIndexOf(":");
        if (colonIndex === -1) {
            return undefined;
        }

        const name = label.substring(0, colonIndex).trim();
        const type = label.substring(colonIndex + 1).trim();

        if (name.length === 0 || type.length === 0) {
            return undefined;
        }

        return { name, type };
    }

    /**
     * Validates a property value label edit.
     * Validates both property name and value with comprehensive type checking.
     *
     * @param label The label text to validate
     * @param element The property label element
     * @returns A validation status if invalid, undefined otherwise
     */
    private validatePropertyValueLabel(label: string, element: GPropertyLabel): ValidationStatusType | undefined {
        const parsed = parseModelPropertyLabel(label, this.modelState.languageServices.parser.Lexer);

        if (typeof parsed === "string") {
            return this.error(parsed);
        }

        if (parsed.value.trim().length === 0) {
            return this.error("Property value cannot be empty.");
        }

        const tokenValidation = this.validateValueTokens(parsed.valueTokens);
        if (tokenValidation != undefined) {
            return tokenValidation;
        }

        const astNode = this.index.getAstNode(element);
        if (astNode == undefined) {
            return undefined;
        }

        const reflection = this.reflection;
        if (!reflection.isInstance(astNode, PropertyAssignment)) {
            return undefined;
        }

        const propertyAssignment = astNode as PartialPropertyAssignment;
        const objectInstance = propertyAssignment.$container as ObjectInstanceType;

        if (objectInstance == undefined) {
            return undefined;
        }

        const classRef = objectInstance.class?.ref;
        if (classRef == undefined) {
            return undefined;
        }

        if (!reflection.isInstance(classRef, Class)) {
            return undefined;
        }

        const classType = classRef as ClassType;

        const classChain = resolveClassChain(classType, reflection);

        const propertyName = parseIdentifier(parsed.name);
        let propertyDef: PropertyType | undefined;

        for (const cls of classChain) {
            propertyDef = (cls.properties ?? []).find((p) => {
                const prop = p as PropertyType;
                return prop.name === propertyName;
            }) as PropertyType | undefined;
            if (propertyDef != undefined) {
                break;
            }
        }

        if (propertyDef == undefined) {
            const nameProvider = this.modelState.languageServices.references.NameProvider;
            return this.error(
                `Property '${propertyName}' does not exist on class '${nameProvider.getName(classRef)}'.`
            );
        }

        return this.validatePropertyValue(parsed.valueTokens, propertyDef);
    }

    /**
     * Validates the token sequence of a value.
     * Ensures tokens form a valid single value or list value.
     *
     * @param tokens The tokens to validate
     * @returns A validation status if invalid, undefined otherwise
     */
    private validateValueTokens(tokens: IToken[]): ValidationStatusType | undefined {
        if (tokens.length === 0) {
            return this.error("Value cannot be empty.");
        }

        if (tokens[0].image === "[") {
            if (tokens[tokens.length - 1]?.image !== "]") {
                return this.error("List value must end with ']'.");
            }
            if (tokens.length === 2) {
                return undefined;
            }
            const innerTokens = tokens.slice(1, -1);
            return this.validateListTokens(innerTokens);
        }

        if (tokens.length !== 1) {
            return this.error("Single value must be a single token (number, string, boolean, or identifier).");
        }

        return this.validateSingleValueToken(tokens[0]);
    }

    /**
     * Validates a single value token.
     *
     * @param token The token to validate
     * @returns A validation status if invalid, undefined otherwise
     */
    private validateSingleValueToken(token: IToken): ValidationStatusType | undefined {
        const validTypes = [STRING.name, INT.name, FLOAT.name, ID.name, BOOLEAN.name];
        if (!validTypes.some((type) => token.tokenType.name.includes(type))) {
            return this.error(`Invalid value: ${token.image}. Expected number, string, boolean, or identifier.`);
        }
        return undefined;
    }

    /**
     * Validates tokens within a list (between brackets).
     *
     * @param tokens The tokens to validate
     * @returns A validation status if invalid, undefined otherwise
     */
    private validateListTokens(tokens: IToken[]): ValidationStatusType | undefined {
        if (tokens.length === 0) {
            return undefined;
        }

        let expectingValue = true;
        for (const token of tokens) {
            if (expectingValue) {
                const validation = this.validateSingleValueToken(token);
                if (validation != undefined) {
                    return validation;
                }
                expectingValue = false;
            } else {
                if (token.image !== ",") {
                    return this.error(`Expected comma between list entries, got: ${token.image}`);
                }
                expectingValue = true;
            }
        }

        if (expectingValue) {
            return this.error("List cannot end with a comma.");
        }

        return undefined;
    }

    /**
     * Validates a property value against its property definition.
     * Includes type checking and multiplicity count validation.
     *
     * @param valueTokens The value tokens to validate
     * @param propertyDef The property definition
     * @returns A validation status if invalid, undefined otherwise
     */
    private validatePropertyValue(valueTokens: IToken[], propertyDef: PropertyType): ValidationStatusType | undefined {
        const reflection = this.reflection;
        const isMultiple = isMultipleMultiplicity(propertyDef.multiplicity, reflection);

        if (isMultiple) {
            if (valueTokens.length === 0 || valueTokens[0].image !== "[") {
                return this.error("Property requires a list value. Expected format: [value1, value2, ...]");
            }

            if (valueTokens[valueTokens.length - 1]?.image !== "]") {
                return this.error("List value must end with ']'.");
            }

            const innerTokens = valueTokens.slice(1, -1);
            const entries = this.extractListEntryTokens(innerTokens);

            const multiplicityValidation = this.validateMultiplicityCount(
                entries.length,
                propertyDef.multiplicity,
                propertyDef.name
            );
            if (multiplicityValidation != undefined) {
                return multiplicityValidation;
            }

            for (const entryTokens of entries) {
                const entryValidation = this.validateSingleValueTokens(entryTokens, propertyDef);
                if (entryValidation != undefined) {
                    return entryValidation;
                }
            }
            return undefined;
        } else {
            if (valueTokens.length > 0 && valueTokens[0].image === "[") {
                return this.error("Property expects a single value, not a list.");
            }
            return this.validateSingleValueTokens(valueTokens, propertyDef);
        }
    }

    /**
     * Validates that the count of values matches the multiplicity constraints.
     *
     * @param count The number of values provided
     * @param multiplicity The multiplicity constraint
     * @param propertyName The property name for error messages
     * @returns A validation status if invalid, undefined otherwise
     */
    private validateMultiplicityCount(
        count: number,
        multiplicity: MultiplicityType | undefined,
        propertyName: string | undefined
    ): ValidationStatusType | undefined {
        const bounds = this.getMultiplicityBounds(multiplicity);

        if (count < bounds.lower) {
            return this.error(
                `Property '${propertyName ?? "unknown"}' requires at least ${bounds.lower} value(s), but ${count} were provided.`
            );
        }

        if (bounds.upper !== undefined && count > bounds.upper) {
            return this.error(
                `Property '${propertyName ?? "unknown"}' allows at most ${bounds.upper} value(s), but ${count} were provided.`
            );
        }

        return undefined;
    }

    /**
     * Gets the lower and upper bounds of a multiplicity.
     *
     * @param multiplicity The multiplicity to analyze
     * @returns Object with lower and upper bounds (upper is undefined for unbounded)
     */
    private getMultiplicityBounds(multiplicity: MultiplicityType | undefined): { lower: number; upper: number | undefined } {
        if (!multiplicity) {
            return { lower: 1, upper: 1 };
        }

        const reflection = this.reflection;

        if (reflection.isInstance(multiplicity, SingleMultiplicity)) {
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

        if (reflection.isInstance(multiplicity, RangeMultiplicity)) {
            const lower = multiplicity.lower;
            const upper = multiplicity.upper === "*" ? undefined : multiplicity.upperNumeric;
            return { lower, upper };
        }

        return { lower: 1, upper: 1 };
    }

    /**
     * Validates a single value (as tokens) against a property type.
     *
     * @param valueTokens The value tokens (should be a single token)
     * @param propertyDef The property definition
     * @returns A validation status if invalid, undefined otherwise
     */
    private validateSingleValueTokens(
        valueTokens: IToken[],
        propertyDef: PropertyType
    ): ValidationStatusType | undefined {
        if (valueTokens.length !== 1) {
            return this.error("Expected a single value token.");
        }

        const token = valueTokens[0];
        const valueStr = token.image;
        const reflection = this.reflection;
        const propertyType = propertyDef.type;

        if (reflection.isInstance(propertyType, PrimitiveType)) {
            return this.validatePrimitiveValue(valueStr, propertyType.name, token);
        } else if (reflection.isInstance(propertyType, EnumTypeReference)) {
            return this.validateEnumValue(valueStr, propertyType);
        }

        return this.error("Unknown property type.");
    }

    /**
     * Validates a primitive type value.
     *
     * @param valueStr The value string
     * @param primitiveType The primitive type name
     * @param token The token representing the value
     * @returns A validation status if invalid, undefined otherwise
     */
    private validatePrimitiveValue(
        valueStr: string,
        primitiveType: MetamodelPrimitiveTypes | undefined,
        token: IToken
    ): ValidationStatusType | undefined {
        const trimmed = valueStr.trim();
        const tokenTypeName = token.tokenType.name;

        switch (primitiveType) {
            case MetamodelPrimitiveTypes.STRING:
                if (tokenTypeName !== STRING.name) {
                    return this.error(`Invalid string value. Expected a string, got '${trimmed}'.`);
                }
                return undefined;

            case MetamodelPrimitiveTypes.BOOLEAN:
                if (tokenTypeName !== BOOLEAN.name) {
                    return this.error(`Invalid boolean value. Expected 'true' or 'false', got '${trimmed}'.`);
                }
                return undefined;

            case MetamodelPrimitiveTypes.INT:
            case MetamodelPrimitiveTypes.LONG:
                if (tokenTypeName !== INT.name) {
                    return this.error(`Invalid integer value. Expected an integer, got '${trimmed}'.`);
                }
                return undefined;

            case MetamodelPrimitiveTypes.FLOAT:
            case MetamodelPrimitiveTypes.DOUBLE:
                if (tokenTypeName !== FLOAT.name) {
                    return this.error(`Invalid floating point value. Expected a number, got '${trimmed}'.`);
                }
                return undefined;

            default:
                return this.error(`Unknown primitive type: ${primitiveType}`);
        }
    }

    /**
     * Validates an enum value.
     *
     * @param valueStr The value string
     * @param enumTypeRef The enum type reference
     * @returns A validation status if invalid, undefined otherwise
     */
    private validateEnumValue(valueStr: string, enumTypeRef: EnumTypeReferenceType): ValidationStatusType | undefined {
        const reflection = this.reflection;
        const enumRef = enumTypeRef.enum?.ref;

        if (enumRef == undefined) {
            return this.error("Enum type not resolved.");
        }

        let enumDef: EnumType | undefined;
        if (reflection.isInstance(enumRef, Enum)) {
            enumDef = enumRef;
        }

        if (enumDef == undefined) {
            return this.error("Invalid enum reference.");
        }

        const trimmed = valueStr.trim();
        const enumEntries = enumDef.entries ?? [];
        const validEntries = enumEntries.map((entry) => entry.name).filter((name) => name != undefined);

        if (!validEntries.includes(trimmed)) {
            return this.error(`Invalid enum value. Expected one of: ${validEntries.join(", ")}, got '${trimmed}'.`);
        }

        return undefined;
    }

    /**
     * Extracts list entry tokens from tokens within a list (between brackets).
     * Splits by comma tokens.
     *
     * @param tokens The tokens within the list (excluding brackets)
     * @returns Array of token arrays, one per list entry
     */
    private extractListEntryTokens(tokens: IToken[]): IToken[][] {
        const entries: IToken[][] = [];
        let current: IToken[] = [];

        for (const token of tokens) {
            if (token.image === ",") {
                if (current.length > 0) {
                    entries.push(current);
                    current = [];
                }
            } else {
                current.push(token);
            }
        }

        if (current.length > 0) {
            entries.push(current);
        }

        return entries;
    }
}

/**
 * Parses a property assignment label into name and value parts using the lexer.
 *
 * @param label The label text
 * @param lexer The lexer to use for tokenization
 * @returns Parsed name, value, and value tokens, or an error string if invalid
 */
export function parseModelPropertyLabel(
    label: string,
    lexer: Lexer
): { name: string; value: string; valueTokens: IToken[] } | string {
    const result = lexer.tokenize(label);

    if (result.errors.length > 0) {
        return result.errors.map((e) => e.message).join("; ");
    }

    const tokens = result.tokens;
    if (tokens.length === 0) {
        return "Empty label";
    }

    const equalsIndex = tokens.findIndex((t: IToken) => t.image === "=");
    if (equalsIndex === -1) {
        return "Missing '=' in label";
    }

    const nameTokens = tokens.slice(0, equalsIndex);
    if (nameTokens.length === 0) {
        return "Missing name before '='";
    }

    const valueTokens = tokens.slice(equalsIndex + 1);
    if (valueTokens.length === 0) {
        return "Missing value after '='";
    }

    const name = tokensToString(nameTokens, label);
    const value = tokensToString(valueTokens, label);

    if (name.length === 0 || value.length === 0) {
        return "Empty name or value";
    }

    return { name, value, valueTokens };
}

/**
 * Reconstructs a string from tokens using original text positions.
 *
 * @param tokens The tokens to reconstruct
 * @param originalText The original text
 * @returns The reconstructed string
 */
function tokensToString(tokens: IToken[], originalText: string): string {
    if (tokens.length === 0) {
        return "";
    }

    const start = tokens[0].startOffset;
    const lastToken = tokens[tokens.length - 1];
    const end = lastToken.endOffset !== undefined ? lastToken.endOffset + 1 : start + lastToken.image.length;

    return originalText.substring(start, end).trim();
}
