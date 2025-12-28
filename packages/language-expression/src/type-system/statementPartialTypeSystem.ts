import type { TypirLangiumSpecifics } from "typir-langium";
import { PartialTypeSystem, type PrimitiveTypes } from "./partialTypeSystem.js";
import type {
    ForStatementType,
    ForStatementVariableDeclarationType,
    StatementTypes
} from "../grammar/statementTypes.js";
import type { ExpressionTypirServices } from "./services.js";
import type { CustomClassType } from "../typir-extensions/kinds/custom-class/custom-class-type.js";
import type { InferenceProblem, TypeInferenceResultWithoutInferringChildren, ValidationProblemAcceptor } from "typir";
import type { BaseExpressionType, ExpressionTypes } from "../grammar/expressionTypes.js";
import type { CustomValueType } from "../typir-extensions/kinds/custom-value/custom-value-type.js";
import type { ClassType } from "../typir-extensions/config/type.js";
import { type AstNode } from "langium";

/**
 * Partial type syste m implementation for statement-related AST nodes.
 *
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 */
export class StatementPartialTypeSystem<Specifics extends TypirLangiumSpecifics> extends PartialTypeSystem<
    Specifics,
    StatementTypes
> {
    constructor(
        typir: ExpressionTypirServices<Specifics>,
        types: StatementTypes,
        protected readonly expressionTypes: ExpressionTypes,
        protected readonly primitiveTypes: PrimitiveTypes,
        protected readonly nullablePrimitiveTypes: PrimitiveTypes,
        protected readonly voidType: CustomClassType,
        protected readonly iterableType: ClassType
    ) {
        super(typir, types);
    }

    override registerRules(): void {
        this.registerVariableDeclarationRules();
        this.registerControlFlowStatementRules();
        this.registerAssignmentRules();
        this.registerBreakContinueStatementRules();

        this.registerInferenceRule(this.types.forStatementVariableDeclarationType, (node) => {
            return this.inferForStatementVariableDeclarationType(node);
        });
    }

    /**
     * Registers inference and validation rules for variable declaration statements.
     */
    private registerVariableDeclarationRules(): void {
        this.registerInferenceRule(this.types.variableDeclarationStatementType, (node) => {
            if (node.type != undefined) {
                return node.type;
            } else {
                return node.initialValue!;
            }
        });

        this.registerValidationRule(this.types.variableDeclarationStatementType, (node, accept) => {
            if (node.initialValue != undefined && node.type != undefined) {
                const initialValueType = this.inference.inferType(node.initialValue);
                const declarationType = this.inference.inferType(node.type);
                if (Array.isArray(initialValueType) || Array.isArray(declarationType)) {
                    return;
                }
                if (!this.assignability.isAssignable(initialValueType, declarationType)) {
                    accept({
                        $problem: this.validationProblem,
                        languageNode: node,
                        message: `Initial value type '${initialValueType.getName()}' is not assignable to variable type '${declarationType.getName()}'.`,
                        severity: "error"
                    });
                }
            }
        });
    }

    /**
     * Registers validation rules for control flow statements like if, else-if, while, and do-while.
     */
    private registerControlFlowStatementRules(): void {
        this.registerValidationRule(this.types.ifStatementType, (node, accept) => {
            this.validateBooleanCondition(node.condition, accept, "If statement condition must be of type boolean.");
        });

        this.registerValidationRule(this.types.elseIfClauseType, (node, accept) => {
            this.validateBooleanCondition(node.condition, accept, "Else-if clause condition must be of type boolean.");
        });

        this.registerValidationRule(this.types.whileStatementType, (node, accept) => {
            this.validateBooleanCondition(node.condition, accept, "While statement condition must be of type boolean.");
        });
    }

    /**
     * Registers validation rules for assignment statements.
     */
    private registerAssignmentRules(): void {
        this.registerValidationRule(this.types.assignmentStatementType, (node, accept) => {
            const rightType = this.inference.inferType(node.right);
            const leftType = this.inference.inferType(node.left);
            if (Array.isArray(rightType) || Array.isArray(leftType)) {
                return;
            }
            if (!this.assignability.isAssignable(rightType, leftType)) {
                accept({
                    $problem: this.validationProblem,
                    languageNode: node,
                    message: `Cannot assign value of type '${rightType.getName()}' to expression of type '${leftType.getName()}'.`,
                    severity: "error"
                });
            }
            let isReadonly = false;
            if (this.astReflection.isInstance(node.left, this.expressionTypes.identifierExpressionType)) {
                const scope = this.typir.ScopeProvider.getScope(node);
                const entry = scope.getEntry(node.left.name);
                if (entry != undefined && entry.readonly === true) {
                    isReadonly = true;
                }
            } else {
                const expressionType = this.inference.inferType(node.left.expression);
                if (this.typir.factory.CustomValues.isCustomValueType(expressionType)) {
                    const member = expressionType.getMember(node.left.member);
                    if (member != undefined && member.readonly === true) {
                        isReadonly = true;
                    }
                }
            }
            if (isReadonly) {
                accept({
                    $problem: this.validationProblem,
                    languageNode: node,
                    message: `Cannot assign to readonly property.`,
                    severity: "error"
                });
            }
        });
    }

    /**
     * Helper to validate that a condition expression is assignable to boolean.
     * Keeps the repeated logic in one place to reduce duplication.
     *
     * @param condition The condition expression to validate
     * @param accept The validation problem acceptor function
     * @param message The error message to use if validation fails
     */
    private validateBooleanCondition(
        condition: BaseExpressionType,
        accept: ValidationProblemAcceptor<Specifics>,
        message: string
    ): void {
        const conditionType = this.inference.inferType(condition);
        if (Array.isArray(conditionType)) {
            return;
        }
        if (!this.assignability.isAssignable(conditionType, this.primitiveTypes.boolean)) {
            accept({
                $problem: this.validationProblem,
                languageNode: condition,
                message,
                severity: "error"
            });
        }
    }

    /**
     * Infers the type of a for statement variable declaration by examining the iterable type.
     *
     * This method performs the following steps:
     * 1. Validates that the container is a ForStatement
     * 2. Infers the type of the iterable expression
     * 3. Finds the iterable type in the type hierarchy (direct or inherited)
     * 4. Extracts the first generic type argument from the iterable type
     *
     * @param node The for statement variable declaration node
     * @returns The inferred element type from the iterable, or an inference problem if inference fails
     */
    private inferForStatementVariableDeclarationType(
        node: ForStatementVariableDeclarationType
    ): TypeInferenceResultWithoutInferringChildren<Specifics> {
        const { InferenceProblem } = this.typir.context.typir;
        const container = node.$container;

        if (!this.astReflection.isInstance(container, this.types.forStatementType)) {
            return <InferenceProblem<Specifics>>{
                $problem: InferenceProblem,
                languageNode: node,
                location: "For statement variable declaration must be within a for statement.",
                subProblems: []
            };
        }

        const iterableType = this.inferIterableType(node, container);
        if ("$problem" in iterableType) {
            return iterableType;
        }

        const actualIterableType = this.findIterableTypeInHierarchy(node, iterableType);
        if ("$problem" in actualIterableType) {
            return actualIterableType;
        }

        return this.extractFirstGenericTypeArgument(actualIterableType);
    }

    /**
     * Infers the type of the iterable expression and validates it is a CustomValueType.
     *
     * @param node The for statement variable declaration node
     * @param container The containing ForStatement node
     * @returns The inferred iterable type, or an inference problem if inference fails
     */
    private inferIterableType(
        node: ForStatementVariableDeclarationType,
        container: ForStatementType
    ): CustomClassType | InferenceProblem<Specifics> {
        const { InferenceProblem } = this.typir.context.typir;
        const iterableType = this.inference.inferType(container.iterable);

        if (Array.isArray(iterableType)) {
            return <InferenceProblem<Specifics>>{
                $problem: InferenceProblem,
                languageNode: node,
                location: "Cannot infer type of iterable expression.",
                subProblems: iterableType
            };
        }

        if (!this.typir.factory.CustomClasses.isCustomClassType(iterableType)) {
            return <InferenceProblem<Specifics>>{
                $problem: InferenceProblem,
                languageNode: node,
                location: `Type '${iterableType.getName()}' is not iterable.`,
                subProblems: []
            };
        }

        return iterableType;
    }

    /**
     * Finds the iterable type in the type hierarchy, checking both the type itself and its parent types.
     *
     * @param node The for statement variable declaration node
     * @param iterableType The inferred type to search for iterable capability
     * @returns The iterable type from the hierarchy, or an inference problem if not found
     */
    private findIterableTypeInHierarchy(
        node: ForStatementVariableDeclarationType,
        iterableType: CustomClassType
    ): CustomClassType | InferenceProblem<Specifics> {
        const { InferenceProblem } = this.typir.context.typir;

        if (iterableType.details.definition === this.iterableType) {
            return iterableType;
        }

        for (const superClass of iterableType.allSuperClasses) {
            if (superClass.details.definition === this.iterableType) {
                return superClass;
            }
        }

        return <InferenceProblem<Specifics>>{
            $problem: InferenceProblem,
            languageNode: node,
            location: `Type '${iterableType.getName()}' is not iterable.`,
            subProblems: []
        };
    }

    /**
     * Extracts the first generic type argument from an iterable type.
     *
     * @param iterableType The iterable type with generic type arguments
     * @returns The first generic type argument
     * @throws Error if the iterable type has no generic type arguments
     */
    private extractFirstGenericTypeArgument(iterableType: CustomClassType): CustomValueType {
        const typeArgs = iterableType.details.typeArgs;

        if (typeArgs.size !== 1) {
            throw new Error(`Iterable type '${iterableType.getName()}' does not have any generic type arguments.`);
        }

        return typeArgs.values().next().value!;
    }

    /**
     * Registers validation rules for break and continue statements.
     *
     * These statements must be contained (directly or indirectly) within a loop statement
     * (for, while, or do-while). Uses Langium's AstUtils.getContainerOfType to traverse
     * the AST hierarchy.
     */
    private registerBreakContinueStatementRules(): void {
        this.registerValidationRule(this.types.breakStatementType, (node, accept) => {
            this.validateInsideLoop(node, accept, "Break statement must be inside a loop (for, while, or do-while).");
        });

        this.registerValidationRule(this.types.continueStatementType, (node, accept) => {
            this.validateInsideLoop(
                node,
                accept,
                "Continue statement must be inside a loop (for, while, or do-while)."
            );
        });
    }

    /**
     * Helper to validate that a statement is contained within a loop statement.
     *
     * Uses Langium's AstUtils.getContainerOfType to find the nearest ancestor matching
     * one of the loop statement types (for, while, do-while).
     *
     * @param node The statement node to validate
     * @param accept The validation problem acceptor function
     * @param message The error message to use if validation fails
     */
    private validateInsideLoop(node: AstNode, accept: ValidationProblemAcceptor<Specifics>, message: string): void {
        const isInsideLoop =
            this.typir.context.langium.AstUtils.getContainerOfType(
                node,
                (n): n is AstNode =>
                    this.astReflection.isInstance(n, this.types.forStatementType) ||
                    this.astReflection.isInstance(n, this.types.whileStatementType)
            ) !== undefined;

        if (!isInsideLoop) {
            accept({
                $problem: this.validationProblem,
                languageNode: node,
                message,
                severity: "error"
            });
        }
    }
}
