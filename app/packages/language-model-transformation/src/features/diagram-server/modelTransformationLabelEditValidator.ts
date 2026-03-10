import { BaseLabelEditValidator, sharedImport, type GModelIndex } from "@mdeo/language-shared";
import type { GModelElement, ValidationStatus as ValidationStatusType } from "@eclipse-glsp/server";
import { ModelTransformationElementType, PatternModifierKind } from "./model/elementTypes.js";
import type { GPatternInstanceNode } from "./model/patternInstanceNode.js";
import {
    PatternObjectInstance,
    PatternObjectInstanceReference,
    PatternPropertyAssignment,
    PatternVariable,
    type PatternObjectInstanceType,
    type PatternPropertyAssignmentType,
    type PatternVariableType
} from "../../grammar/modelTransformationTypes.js";
import {
    parseInstanceLabel,
    parseVariableLabel,
    parseModelTransformationPropertyLabel as parsePropertyLabel
} from "./modelTransformationLabelParseUtils.js";

export { parseModelTransformationPropertyLabel } from "./modelTransformationLabelParseUtils.js";

const { injectable, inject } = sharedImport("inversify");
const { ValidationStatus, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Validator for label edits in the model transformation diagram.
 *
 * Validates the following label kinds:
 * - Pattern instance name labels: validates format and name uniqueness
 * - Pattern property assignment labels: validates format and modifier constraints
 * - Where clause labels: validates `where ` prefix (expression is not further validated)
 * - Variable declaration labels: validates format and name uniqueness
 */
@injectable()
export class ModelTransformationLabelEditValidator extends BaseLabelEditValidator {
    /**
     * Injected model index for resolving AST nodes from graph element IDs.
     */
    @inject(GModelIndexKey)
    protected readonly index!: GModelIndex;

    /**
     * Entry point for label edit validation.
     * Dispatches to the appropriate sub-validator based on element type.
     *
     * @param label - The new label text submitted by the user
     * @param element - The graph model element being edited
     * @returns Validation status indicating success or failure
     */
    override validate(label: string, element: GModelElement): ValidationStatusType {
        switch (element.type) {
            case ModelTransformationElementType.LABEL_PATTERN_INSTANCE_NAME:
                return this.validateInstanceLabel(label, element) ?? ValidationStatus.NONE;
            case ModelTransformationElementType.LABEL_PATTERN_PROPERTY:
                return this.validatePropertyLabel(label, element) ?? ValidationStatus.NONE;
            case ModelTransformationElementType.LABEL_WHERE_CLAUSE:
                return this.validateWhereClauseLabel(label) ?? ValidationStatus.NONE;
            case ModelTransformationElementType.LABEL_VARIABLE:
                return this.validateVariableLabel(label, element) ?? ValidationStatus.NONE;
            case ModelTransformationElementType.LABEL_PATTERN_LINK_MODIFIER:
                return this.validateModifierLabel(label) ?? ValidationStatus.NONE;
            case ModelTransformationElementType.LABEL_PATTERN_MODIFIER:
                return this.validateModifierLabel(label) ?? ValidationStatus.NONE;
            case ModelTransformationElementType.LABEL_CONTROL_FLOW:
                return this.validateControlFlowLabel(label) ?? ValidationStatus.NONE;
            default:
                return ValidationStatus.NONE;
        }
    }

    /**
     * Validates a pattern instance name label.
     *
     * Accepts both `name : type` and name-only formats (the latter is used
     * when the class reference is currently unresolved).  Validates that the
     * name is a valid identifier and globally unique in the transformation.
     *
     * @param label - The label text to validate
     * @param element - The graph model element (instance node label)
     * @returns A validation status if invalid, undefined if valid
     */
    private validateInstanceLabel(label: string, element: GModelElement): ValidationStatusType | undefined {
        const parsed = parseInstanceLabel(label);
        const name = parsed != undefined ? parsed.name : label.trim();

        if (parsed == undefined && label.includes(":")) {
            return this.error("Invalid instance format. Expected: name : type or just name");
        }

        const nameValidation = this.validateIdentifier(name, "Instance name");
        if (nameValidation != undefined) {
            return nameValidation;
        }

        if (parsed != undefined) {
            const typeValidation = this.validateIdentifier(parsed.type, "Instance type");
            if (typeValidation != undefined) {
                return typeValidation;
            }
        }

        const astNode = this.index.getAstNode(element);
        const currentName =
            astNode != undefined && this.reflection.isInstance(astNode, PatternObjectInstance)
                ? (astNode as PatternObjectInstanceType).name
                : undefined;

        if (currentName !== name) {
            if (this.nameExistsElsewhere(name, currentName)) {
                return this.error(`A pattern instance or variable named '${name}' already exists.`);
            }
        }

        return undefined;
    }

    /**
     * Validates a pattern property assignment label.
     *
     * Checks the `propName = expr` or `propName == expr` format, then validates
     * that the chosen operator is permitted for the enclosing instance's modifier:
     * only NONE-modifier and CREATE instances may use `=` (assignment);
     * all other modifiers must use `==` (comparison).
     *
     * @param label - The label text to validate
     * @param element - The graph model element (property label)
     * @returns A validation status if invalid, undefined if valid
     */
    private validatePropertyLabel(label: string, element: GModelElement): ValidationStatusType | undefined {
        if (label.trim().length === 0) {
            return undefined;
        }

        const parsed = parsePropertyLabel(label);
        if (typeof parsed === "string") {
            return this.error(parsed);
        }

        const nameValidation = this.validateIdentifier(parsed.name, "Property name");
        if (nameValidation != undefined) {
            return nameValidation;
        }

        if (parsed.value.trim().length === 0) {
            return this.error("Property value expression cannot be empty.");
        }

        const modifier = this.getEnclosingModifier(element);
        return this.validateOperatorForModifier(parsed.operator, modifier);
    }

    /**
     * Validates a where clause label.
     *
     * Only checks that the text starts with the required `where ` prefix and
     * that the expression is non-empty.  The expression itself is not further
     * validated (parsing expressions is out of scope for now).
     *
     * @param label - The label text to validate
     * @returns A validation status if invalid, undefined if valid
     */
    private validateWhereClauseLabel(label: string): ValidationStatusType | undefined {
        if (label.trim().length === 0) {
            return undefined;
        }

        if (!label.startsWith("where ")) {
            return this.error("Where clause must start with 'where '.");
        }
        const expression = label.substring("where ".length).trim();
        if (expression.length === 0) {
            return this.error("Where clause expression cannot be empty.");
        }
        return undefined;
    }

    /**
     * Validates a variable declaration label.
     *
     * Checks the `var name[: type] = expr` format and validates that the name is
     * a valid identifier and globally unique within the transformation.  The
     * expression value is not further validated.
     *
     * @param label - The label text to validate
     * @param element - The graph model element (variable label)
     * @returns A validation status if invalid, undefined if valid
     */
    private validateVariableLabel(label: string, element: GModelElement): ValidationStatusType | undefined {
        if (label.trim().length === 0) {
            return undefined;
        }

        const parsed = parseVariableLabel(label);
        if (parsed == undefined) {
            return this.error("Invalid variable format. Expected: var name[: type] = expression");
        }

        const nameValidation = this.validateIdentifier(parsed.name, "Variable name");
        if (nameValidation != undefined) {
            return nameValidation;
        }

        if (parsed.value.trim().length === 0) {
            return this.error("Variable value expression cannot be empty.");
        }

        const trimmedName = parsed.name.trim();
        const astNode = this.index.getAstNode(element);
        const currentName =
            astNode != undefined && this.reflection.isInstance(astNode, PatternVariable)
                ? (astNode as PatternVariableType).name
                : undefined;

        if (currentName !== trimmedName) {
            if (this.nameExistsElsewhere(trimmedName, currentName)) {
                return this.error(`A variable or instance named '${trimmedName}' already exists.`);
            }
        }

        return undefined;
    }

    /**
     * Checks whether an operator is valid for the given modifier kind.
     *
     * Only NONE (no modifier) and CREATE allow assignments (`=`).
     * All other modifiers (DELETE, FORBID, REQUIRE) must use comparison (`==`).
     * PatternObjectInstanceReference nodes (which have no modifier field) also
     * only allow comparisons.
     *
     * @param operator - The operator string (`=` or `==`)
     * @param modifier - The modifier kind of the enclosing instance, or 'reference' for references
     * @returns A validation status if the operator is not permitted, undefined otherwise
     */
    private validateOperatorForModifier(
        operator: string,
        modifier: PatternModifierKind | "reference" | undefined
    ): ValidationStatusType | undefined {
        if (modifier === "reference") {
            if (operator === "=") {
                return this.error(
                    "Assignment ('=') is not allowed in an instance reference. Use '==' for comparisons."
                );
            }
            return undefined;
        }

        if (operator === "=") {
            const allowed = modifier === PatternModifierKind.NONE || modifier === PatternModifierKind.CREATE;
            if (!allowed) {
                return this.error(
                    `Assignment ('=') is not allowed with modifier '${modifier ?? "none"}'. ` +
                        `Only 'none' (no modifier) and 'create' instances support assignment. Use '==' for comparisons.`
                );
            }
        }

        return undefined;
    }

    /**
     * Resolves the modifier kind of the instance that contains the given property label element.
     *
     * Walks up the graph model containment hierarchy to find the nearest
     * `GPatternInstanceNode` and returns its modifier.  Returns `'reference'`
     * when the property belongs to a `PatternObjectInstanceReference` rather
     * than a full `PatternObjectInstance`.
     *
     * @param element - The property label graph element
     * @returns The modifier kind, `'reference'`, or undefined if it cannot be resolved
     */
    private getEnclosingModifier(element: GModelElement): PatternModifierKind | "reference" | undefined {
        const astNode = this.index.getAstNode(element);
        if (astNode != undefined) {
            if (this.reflection.isInstance(astNode, PatternPropertyAssignment)) {
                const prop = astNode as PatternPropertyAssignmentType;
                const container = prop.$container;
                if (container != undefined) {
                    if (this.reflection.isInstance(container, PatternObjectInstance)) {
                        const instance = container as PatternObjectInstanceType;
                        return modifierStringToKind(instance.modifier?.modifier);
                    }
                    if (this.reflection.isInstance(container, PatternObjectInstanceReference)) {
                        return "reference";
                    }
                }
            }
        }

        return this.getModifierFromParentNode(element);
    }

    /**
     * Traverses the GModel element tree upward to find the first
     * `GPatternInstanceNode` parent and return its stored modifier.
     *
     * @param element - The element to start traversal from
     * @returns The modifier kind from the parent node, or undefined if not found
     */
    private getModifierFromParentNode(element: GModelElement): PatternModifierKind | undefined {
        let current = element.parent;
        while (current != undefined) {
            if (current.type === ModelTransformationElementType.NODE_PATTERN_INSTANCE) {
                const instanceNode = current as unknown as GPatternInstanceNode;
                return instanceNode.modifier;
            }
            current = current.parent;
        }
        return undefined;
    }

    /**
     * Checks whether any pattern instance or variable with the given name already exists
     * in the current diagram model, excluding the element with `currentName`.
     *
     * Searches all `GPatternInstanceNode` elements for a matching instance name,
     * and all `LABEL_VARIABLE` labels for a matching variable name.
     *
     * @param name - The name to check for existence
     * @param currentName - The current name of the element being renamed (excluded from the check)
     * @returns `true` if the name is already in use by another element, `false` otherwise
     */
    private nameExistsElsewhere(name: string, currentName: string | undefined): boolean {
        const instanceNodes = this.findAllByType(ModelTransformationElementType.NODE_PATTERN_INSTANCE);
        const instanceMatch = instanceNodes.some((node) => {
            const instanceNode = node as unknown as GPatternInstanceNode;
            return instanceNode.name === name && instanceNode.name !== currentName;
        });
        if (instanceMatch) {
            return true;
        }

        const variableLabels = this.findAllByType(ModelTransformationElementType.LABEL_VARIABLE);
        return variableLabels.some((label) => {
            const astNode = this.index.getAstNode(label);
            if (astNode != undefined && this.reflection.isInstance(astNode, PatternVariable)) {
                const varNode = astNode as PatternVariableType;
                return varNode.name === name && varNode.name !== currentName;
            }
            return false;
        });
    }

    /**
     * Collects all graph model elements with the specified type from the entire model root.
     *
     * @param type - The element type to collect
     * @returns An array of all found elements
     */
    private findAllByType(type: string): GModelElement[] {
        const results: GModelElement[] = [];
        this.collectByType(this.modelState.root, type, results);
        return results;
    }

    /**
     * Recursively collects all graph model elements with the specified type.
     *
     * @param element - The root element to start the traversal from
     * @param type - The element type to collect
     * @param results - The array to add matching elements to
     */
    private collectByType(element: GModelElement, type: string, results: GModelElement[]): void {
        if (element.type === type) {
            results.push(element);
        }
        for (const child of element.children ?? []) {
            this.collectByType(child, type, results);
        }
    }

    /**
     * Validates a pattern modifier label (pattern instance or link modifier).
     *
     * Accepts any of: create, delete, forbid, require, or empty (none).
     * The value may be wrapped in « » guillemets, which are stripped before comparison.
     *
     * @param label - The label text to validate
     * @returns A validation status if invalid, undefined if valid
     */
    private validateModifierLabel(label: string): ValidationStatusType | undefined {
        const modifier = this.parseModifierText(label);
        if (!this.isValidModifier(modifier)) {
            return this.error("Modifier must be one of: create, delete, forbid, require (or empty for none).");
        }
        return undefined;
    }

    /**
     * Strips guillemet characters and trims the label to extract the modifier value.
     */
    private parseModifierText(label: string): string {
        return label
            .replace(/\u00ab/g, "")
            .replace(/\u00bb/g, "")
            .trim()
            .toLowerCase();
    }

    /**
     * Checks whether a modifier string is one of the permitted values.
     */
    private isValidModifier(modifier: string): boolean {
        return (
            modifier === "" ||
            modifier === "none" ||
            modifier === "create" ||
            modifier === "delete" ||
            modifier === "forbid" ||
            modifier === "require"
        );
    }

    /**
     * Validates a control flow edge label (condition expression).
     *
     * Only checks that the expression is non-empty; no further parsing is performed.
     *
     * @param label - The label text to validate
     * @returns A validation status if invalid, undefined if valid
     */
    private validateControlFlowLabel(label: string): ValidationStatusType | undefined {
        if (label.trim().length === 0) {
            return this.error("Condition expression cannot be empty.");
        }
        return undefined;
    }
}

/**
 * Converts a modifier string value from the AST to a {@link PatternModifierKind} enum value.
 *
 * @param modifier - The raw string modifier value (e.g., `"create"`, `"delete"`)
 * @returns The corresponding {@link PatternModifierKind}
 */
function modifierStringToKind(modifier: string | undefined): PatternModifierKind {
    switch (modifier) {
        case "create":
            return PatternModifierKind.CREATE;
        case "delete":
            return PatternModifierKind.DELETE;
        case "forbid":
            return PatternModifierKind.FORBID;
        case "require":
            return PatternModifierKind.REQUIRE;
        default:
            return PatternModifierKind.NONE;
    }
}
