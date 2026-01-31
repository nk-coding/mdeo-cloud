import type { ValidationAcceptor, ValidationChecks, AstNode, MultiMap as MultiMapType } from "langium";
import type { ExtendedLangiumServices } from "@mdeo/language-common";
import { Property, resolveClassChain, type ClassType, type PropertyType } from "@mdeo/language-metamodel";
import { BaseModelValidator } from "@mdeo/language-model";
import {
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    IfExpressionStatement,
    WhileExpressionStatement,
    PatternObjectInstance,
    PatternVariable,
    type ModelTransformationType,
    type PatternObjectInstanceType,
    type PatternLinkType,
    type PatternType,
    type TransformationStatementType,
    type IfExpressionStatementType,
    type WhileExpressionStatementType,
    type ElseIfBranchType,
    type IfMatchStatementType
} from "../grammar/modelTransformationTypes.js";
import { DeletedInstanceAnalyzer } from "./deletedInstanceAnalyzer.js";
import { sharedImport } from "@mdeo/language-shared";

const { MultiMap } = sharedImport("langium");

/**
 * Interface for named elements (objects and variables).
 */
interface NamedElement {
    name: string;
    node: AstNode;
    type: "object" | "variable";
}

/**
 * Identifier expression type for validation.
 */
type IdentifierExpressionType = AstNode & { name: string };

/**
 * Interface mapping for model transformation AST types used in validation checks.
 */
interface ModelTransformationAstTypes {
    ModelTransformation: ModelTransformationType;
    PatternObjectInstance: PatternObjectInstanceType;
    PatternLink: PatternLinkType;
    ModelTransformationIdentifierExpression: IdentifierExpressionType;
}

/**
 * Registers validation checks for the model transformation language.
 *
 * @param services The language services
 */
export function registerModelTransformationValidationChecks(services: ExtendedLangiumServices): void {
    const registry = services.validation.ValidationRegistry;
    const validator = new ModelTransformationValidator(services);

    const checks: ValidationChecks<ModelTransformationAstTypes> = {
        ModelTransformation: validator.validateTransformation.bind(validator),
        PatternObjectInstance: validator.validateObjectInstance.bind(validator),
        PatternLink: validator.validateLink.bind(validator),
        ModelTransformationIdentifierExpression: validator.validateIdentifierExpression.bind(validator)
    };

    registry.register(checks, validator);
}

/**
 * Validator for model transformation language constructs.
 * Provides validation for global uniqueness, required properties, link validation,
 * and control flow validation for deleted object instances.
 */
export class ModelTransformationValidator extends BaseModelValidator {
    private readonly deletedInstanceAnalyzer: DeletedInstanceAnalyzer;

    constructor(private readonly services: ExtendedLangiumServices) {
        super(services.shared.AstReflection);
        this.deletedInstanceAnalyzer = new DeletedInstanceAnalyzer(this.reflection, services.shared);
    }

    /**
     * Validates the entire transformation for global uniqueness of names.
     *
     * @param transformation The model transformation to validate
     * @param accept The validation acceptor
     */
    validateTransformation(transformation: ModelTransformationType, accept: ValidationAcceptor): void {
        const allNames = new MultiMap<string, NamedElement>();
        this.collectAllNames(transformation, allNames);
        this.reportDuplicateNames(allNames, accept);
    }

    /**
     * Validates an identifier expression for references to deleted object instances.
     * Uses control flow analysis to detect if the referenced instance might have been deleted.
     *
     * @param identifier The identifier expression to validate
     * @param accept The validation acceptor
     */
    validateIdentifierExpression(identifier: IdentifierExpressionType, accept: ValidationAcceptor): void {
        this.deletedInstanceAnalyzer.validateIdentifierReference(identifier, accept);
    }

    /**
     * Collects all variable and object instance names from the transformation.
     *
     * @param transformation The transformation to collect names from
     * @param names The multimap to add collected names to
     */
    private collectAllNames(transformation: ModelTransformationType, names: MultiMapType<string, NamedElement>): void {
        for (const statement of transformation.statements ?? []) {
            this.collectNamesFromStatement(statement, names);
        }
    }

    /**
     * Collects names from a transformation statement recursively.
     *
     * @param statement The statement to collect names from
     * @param names The multimap to add collected names to
     */
    private collectNamesFromStatement(
        statement: TransformationStatementType,
        names: MultiMapType<string, NamedElement>
    ): void {
        if (this.reflection.isInstance(statement, MatchStatement)) {
            this.collectNamesFromPattern(statement.pattern, names);
        } else if (this.reflection.isInstance(statement, IfMatchStatement)) {
            this.collectNamesFromIfMatchStatement(statement, names);
        } else if (this.reflection.isInstance(statement, WhileMatchStatement)) {
            this.collectNamesFromMatchBlock(statement, names);
        } else if (this.reflection.isInstance(statement, UntilMatchStatement)) {
            this.collectNamesFromMatchBlock(statement, names);
        } else if (this.reflection.isInstance(statement, ForMatchStatement)) {
            this.collectNamesFromMatchBlock(statement, names);
        } else if (this.reflection.isInstance(statement, IfExpressionStatement)) {
            this.collectNamesFromIfExpressionStatement(statement, names);
        } else if (this.reflection.isInstance(statement, WhileExpressionStatement)) {
            this.collectNamesFromWhileExpressionStatement(statement, names);
        }
    }

    /**
     * Collects names from a match-based block statement (while/until/for).
     *
     * @param statement The match block statement
     * @param names The multimap to add collected names to
     */
    private collectNamesFromMatchBlock(
        statement: { pattern: PatternType; doBlock?: { statements?: TransformationStatementType[] } },
        names: MultiMapType<string, NamedElement>
    ): void {
        this.collectNamesFromPattern(statement.pattern, names);
        this.collectNamesFromStatementsScope(statement.doBlock, names);
    }

    /**
     * Collects names from an IfMatchStatement.
     *
     * @param statement The if-match statement
     * @param names The multimap to add collected names to
     */
    private collectNamesFromIfMatchStatement(
        statement: IfMatchStatementType,
        names: MultiMapType<string, NamedElement>
    ): void {
        this.collectNamesFromPattern(statement.ifBlock.pattern, names);
        this.collectNamesFromStatementsScope(statement.ifBlock.thenBlock, names);
        if (statement.elseBlock) {
            this.collectNamesFromStatementsScope(statement.elseBlock, names);
        }
    }

    /**
     * Collects names from an IfExpressionStatement.
     *
     * @param statement The if-expression statement
     * @param names The multimap to add collected names to
     */
    private collectNamesFromIfExpressionStatement(
        statement: IfExpressionStatementType,
        names: MultiMapType<string, NamedElement>
    ): void {
        this.collectNamesFromStatementsScope(statement.thenBlock, names);
        for (const branch of statement.elseIfBranches ?? []) {
            this.collectNamesFromElseIfBranch(branch, names);
        }
        if (statement.elseBlock) {
            this.collectNamesFromStatementsScope(statement.elseBlock, names);
        }
    }

    /**
     * Collects names from an ElseIfBranch.
     *
     * @param branch The else-if branch
     * @param names The multimap to add collected names to
     */
    private collectNamesFromElseIfBranch(branch: ElseIfBranchType, names: MultiMapType<string, NamedElement>): void {
        this.collectNamesFromStatementsScope(branch.block, names);
    }

    /**
     * Collects names from a WhileExpressionStatement.
     *
     * @param statement The while-expression statement
     * @param names The multimap to add collected names to
     */
    private collectNamesFromWhileExpressionStatement(
        statement: WhileExpressionStatementType,
        names: MultiMapType<string, NamedElement>
    ): void {
        this.collectNamesFromStatementsScope(statement.block, names);
    }

    /**
     * Collects names from a statements scope.
     *
     * @param scope The scope containing statements
     * @param names The multimap to add collected names to
     */
    private collectNamesFromStatementsScope(
        scope: { statements?: TransformationStatementType[] } | undefined,
        names: MultiMapType<string, NamedElement>
    ): void {
        for (const statement of scope?.statements ?? []) {
            this.collectNamesFromStatement(statement, names);
        }
    }

    /**
     * Collects names from a pattern.
     *
     * @param pattern The pattern to collect names from
     * @param names The multimap to add collected names to
     */
    private collectNamesFromPattern(pattern: PatternType | undefined, names: MultiMapType<string, NamedElement>): void {
        for (const element of pattern?.elements ?? []) {
            if (this.reflection.isInstance(element, PatternObjectInstance)) {
                if (element.name) {
                    names.add(element.name, { name: element.name, node: element, type: "object" });
                }
            } else if (this.reflection.isInstance(element, PatternVariable)) {
                if (element.name) {
                    names.add(element.name, { name: element.name, node: element, type: "variable" });
                }
            }
        }
    }

    /**
     * Reports duplicate names found in the transformation.
     *
     * @param names The multimap of names to check
     * @param accept The validation acceptor
     */
    private reportDuplicateNames(names: MultiMapType<string, NamedElement>, accept: ValidationAcceptor): void {
        for (const [name, elements] of names.entriesGroupedByKey()) {
            if (elements.length > 1) {
                for (const element of elements) {
                    accept("error", `Duplicate name '${name}': ${element.type} names must be globally unique.`, {
                        node: element.node,
                        property: "name"
                    });
                }
            }
        }
    }

    /**
     * Validates a pattern object instance.
     *
     * @param obj The pattern object instance to validate
     * @param accept The validation acceptor
     */
    validateObjectInstance(obj: PatternObjectInstanceType, accept: ValidationAcceptor): void {
        this.validateClassNotAbstract(obj, accept);
        this.validateRequiredPropertiesForCreate(obj, accept);
    }

    /**
     * Validates that the class is not abstract when using create modifier.
     *
     * @param obj The pattern object instance
     * @param accept The validation acceptor
     */
    private validateClassNotAbstract(obj: PatternObjectInstanceType, accept: ValidationAcceptor): void {
        const classType = this.getClassTypeFromObject(obj);
        if (!classType) {
            return;
        }

        const typedClass = classType as { isAbstract?: boolean; name?: string };
        if (typedClass.isAbstract && obj.modifier?.modifier === "create") {
            accept("error", `Cannot create instance of abstract class '${typedClass.name}'.`, {
                node: obj,
                property: "class"
            });
        }
    }

    /**
     * Validates that all required properties are assigned for objects with create modifier.
     *
     * @param obj The pattern object instance to validate
     * @param accept The validation acceptor
     */
    private validateRequiredPropertiesForCreate(obj: PatternObjectInstanceType, accept: ValidationAcceptor): void {
        if (obj.modifier?.modifier !== "create") {
            return;
        }

        const classType = this.getClassTypeFromObject(obj);
        if (!classType) {
            return;
        }

        const assignedNames = this.collectAssignedPropertyNames(obj);
        this.reportMissingRequiredProperties(classType, assignedNames, obj, accept);
    }

    /**
     * Gets the resolved class type from a pattern object instance.
     *
     * @param obj The pattern object instance
     * @returns The resolved class type, or undefined if not resolvable
     */
    private getClassTypeFromObject(obj: PatternObjectInstanceType): ClassType | undefined {
        const classRef = obj.class?.ref;
        if (!classRef) {
            return undefined;
        }
        return this.resolveToClass(classRef);
    }

    /**
     * Collects the names of all assigned properties from an object.
     *
     * @param obj The pattern object instance
     * @returns Set of assigned property names
     */
    private collectAssignedPropertyNames(obj: PatternObjectInstanceType): Set<string> {
        const assignedPropertyNames = new Set<string>();

        for (const propAssign of obj.properties ?? []) {
            const propRef = propAssign.name?.ref;
            if (propRef && this.reflection.isInstance(propRef, Property)) {
                const prop = propRef as PropertyType;
                if (prop.name) {
                    assignedPropertyNames.add(prop.name);
                }
            }
        }

        return assignedPropertyNames;
    }

    /**
     * Reports errors for missing required properties.
     *
     * @param classType The class type to check
     * @param assignedNames Set of already assigned property names
     * @param obj The object to report errors on
     * @param accept The validation acceptor
     */
    private reportMissingRequiredProperties(
        classType: ClassType,
        assignedNames: Set<string>,
        obj: PatternObjectInstanceType,
        accept: ValidationAcceptor
    ): void {
        const classChain = resolveClassChain(classType, this.reflection);

        for (const cls of classChain) {
            const typedClass = cls as { properties?: PropertyType[] };
            for (const prop of typedClass.properties ?? []) {
                if (!prop.name) {
                    continue;
                }
                if (!assignedNames.has(prop.name) && this.isRequiredProperty(prop)) {
                    accept("error", `Required property '${prop.name}' must be assigned for create modifier.`, {
                        node: obj,
                        property: "name"
                    });
                }
            }
        }
    }

    /**
     * Validates a pattern link.
     *
     * @param link The pattern link to validate
     * @param accept The validation acceptor
     */
    validateLink(link: PatternLinkType, accept: ValidationAcceptor): void {
        const source = link.source;
        const target = link.target;

        if (!source?.object?.ref || !target?.object?.ref) {
            return;
        }

        const sourceObj = source.object.ref as PatternObjectInstanceType;
        const targetObj = target.object.ref as PatternObjectInstanceType;

        this.validateLinkBase(link, sourceObj, targetObj, accept);
    }
}
