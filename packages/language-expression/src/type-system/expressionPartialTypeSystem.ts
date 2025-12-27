import type { TypirLangiumSpecifics } from "typir-langium";
import type { ExpressionTypes } from "../grammar/expressionTypes.js";
import type { CustomValueType } from "../typir-extensions/kinds/custom-value/custom-value-type.js";
import { PartialTypeSystem, type PrimitiveTypes } from "./partialTypeSystem.js";
import { inferMemberAccess } from "../typir-extensions/rules/inferMemberAccess.js";
import { inferCall } from "../typir-extensions/rules/inferCall.js";
import { validateMemberAccess } from "../typir-extensions/rules/validateMemberAccess.js";
import { validateCall } from "../typir-extensions/rules/validateCall.js";
import { findCommonParentType } from "../typir-extensions/rules/commonParentType.js";
import { assertUnreachable } from "@mdeo/language-common";
import type { ExpressionTypirServices } from "./services.js";

/**
 * Partial type system implementation for expression-related type inference and validation.
 * This class handles type inference and validation rules for literals, unary/binary/ternary expressions,
 * member access, and call expressions.
 *
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 */
export class ExpressionPartialTypeSystem<Specifics extends TypirLangiumSpecifics> extends PartialTypeSystem<
    Specifics,
    ExpressionTypes
> {
    constructor(
        typir: ExpressionTypirServices<Specifics>,
        types: ExpressionTypes,
        protected readonly primitiveTypes: PrimitiveTypes,
        protected readonly nullablePrimitiveTypes: PrimitiveTypes
    ) {
        super(typir, types);
    }

    override registerRules(): void {
        this.registerLiteralRules();
        this.registerMemberAccessRules();
        this.registerCallRules();
        this.registerUnaryExpressionRules();
        this.registerBinaryExpressionRules();
        this.registerTernaryExpressionRules();
    }

    /**
     * Registers type inference rules for literal expressions.
     * Handles int, long, float, double, string, and boolean literals.
     */
    private registerLiteralRules(): void {
        this.registerInferenceRule(this.types.intLiteralExpressionType, () => this.primitiveTypes.int);
        this.registerInferenceRule(this.types.longLiteralExpressionType, () => this.primitiveTypes.long);
        this.registerInferenceRule(this.types.floatLiteralExpressionType, () => this.primitiveTypes.float);
        this.registerInferenceRule(this.types.doubleLiteralExpressionType, () => this.primitiveTypes.double);
        this.registerInferenceRule(this.types.stringLiteralExpressionType, () => this.primitiveTypes.string);
        this.registerInferenceRule(this.types.booleanLiteralExpressionType, () => this.primitiveTypes.boolean);
        this.registerInferenceRule(this.types.nullLiteralExpressionType, () => this.typir.factory.CustomNull.getOrCreate());
    }

    /**
     * Registers type inference and validation rules for member access expressions.
     * Handles property and method access, including null-chaining operators.
     */
    private registerMemberAccessRules(): void {
        this.registerInferenceRule(this.types.memberAccessExpressionType, (node) => {
            const inferResult = inferMemberAccess(node, node.expression, node.member, this.typir);
            if (node.isNullChaining && this.typir.factory.CustomValues.isCustomValueType(inferResult)) {
                return inferResult.asNullable;
            }
            return inferResult;
        });

        this.registerValidationRule(this.types.memberAccessExpressionType, (node, accept) =>
            validateMemberAccess(node, node.expression, node.member, node.isNullChaining, this.typir).forEach(accept)
        );
    }

    /**
     * Registers type inference and validation rules for call expressions.
     * Handles function and method calls with generic type arguments.
     */
    private registerCallRules(): void {
        this.registerInferenceRule(this.types.callExpressionType, (node) => {
            const inferResult = inferCall(
                node,
                node.expression,
                node.genericArgs.typeArguments,
                node.arguments,
                this.typir
            );
            if (this.typir.factory.CustomValues.isCustomValueType(inferResult)) {
                const expression = node.expression;
                if (this.astReflection.isInstance(expression, this.types.memberAccessExpressionType)) {
                    if ((expression as any).isNullChaining) {
                        return inferResult.asNullable;
                    }
                }
            }
            return inferResult;
        });

        this.registerValidationRule(this.types.callExpressionType, (node, accept) =>
            validateCall(node, node.expression, node.genericArgs.typeArguments, node.arguments, this.typir).forEach(
                accept
            )
        );
    }

    /**
     * Registers type inference and validation rules for unary expressions.
     * Handles unary minus (-) and logical not (!) operators.
     */
    private registerUnaryExpressionRules(): void {
        this.registerInferenceRule(this.types.unaryExpressionType, (node) => {
            const expressionType = this.inference.inferType(node.expression);
            if (Array.isArray(expressionType)) {
                return expressionType[0];
            }
            if (node.operator === "-") {
                if (this.assignability.isAssignable(expressionType, this.primitiveTypes.double)) {
                    return expressionType;
                } else {
                    return {
                        $problem: this.inferenceProblem,
                        languageNode: node,
                        location: `Type '${expressionType.getName()}' is not valid for unary operator '-'.`,
                        subProblems: []
                    };
                }
            } else if (node.operator === "!") {
                if (this.assignability.isAssignable(expressionType, this.primitiveTypes.boolean)) {
                    return this.primitiveTypes.boolean;
                } else {
                    return {
                        $problem: this.inferenceProblem,
                        languageNode: node,
                        location: `Type '${expressionType.getName()}' is not valid for unary operator '!'.`,
                        subProblems: []
                    };
                }
            } else {
                assertUnreachable(node.operator);
            }
        });

        this.registerValidationRule(this.types.unaryExpressionType, (node, accept) => {
            const type = this.inference.inferType(node);
            if (!Array.isArray(type)) {
                return;
            }
            const expressionType = this.inference.inferType(node.expression);
            if (Array.isArray(expressionType)) {
                return;
            }
            accept({
                $problem: this.validationProblem,
                severity: "error",
                languageNode: node,
                message: `Type '${expressionType.getName()}' is not valid for unary operator '${node.operator}'.`,
                subProblems: []
            });
        });
    }

    /**
     * Registers type inference and validation rules for binary expressions.
     * Handles arithmetic (+, -, *, /, %), comparison (<, >, <=, >=), equality (==, !=),
     * and logical (&&, ||) operators.
     */
    private registerBinaryExpressionRules(): void {
        this.registerInferenceRule(this.types.binaryExpressionType, (node) => {
            const operator = node.operator;
            const leftType = this.inference.inferType(node.left);
            const rightType = this.inference.inferType(node.right);
            if (Array.isArray(leftType)) {
                return leftType[0];
            }
            if (Array.isArray(rightType)) {
                return rightType[0];
            }

            if (
                !this.typir.factory.CustomValues.isCustomValueType(leftType) ||
                !this.typir.factory.CustomValues.isCustomValueType(rightType)
            ) {
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: `Binary operators require two custom value types.`,
                    subProblems: []
                };
            }

            if (operator === "+") {
                if (this.isNumberType(leftType) && this.isNumberType(rightType)) {
                    return this.findClosestNumberType(leftType, rightType);
                } else if (
                    this.assignability.isAssignable(leftType, this.primitiveTypes.string) ||
                    this.assignability.isAssignable(rightType, this.primitiveTypes.string)
                ) {
                    return this.primitiveTypes.string;
                } else {
                    return {
                        $problem: this.inferenceProblem,
                        languageNode: node,
                        location: `Types '${leftType.getName()}' and '${rightType.getName()}' are not valid for operator '+'.`,
                        subProblems: []
                    };
                }
            } else if (
                operator === "-" ||
                operator === "*" ||
                operator === "/" ||
                operator === "%" ||
                operator === "<" ||
                operator === ">" ||
                operator === "<=" ||
                operator === ">="
            ) {
                if (this.isNumberType(leftType) && this.isNumberType(rightType)) {
                    const resultType = this.findClosestNumberType(leftType, rightType);
                    if (operator === "<" || operator === ">" || operator === "<=" || operator === ">=") {
                        return this.primitiveTypes.boolean;
                    }
                    return resultType;
                }
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: `Operator '${operator}' requires two numbers.`,
                    subProblems: []
                };
            } else if (operator === "&&" || operator === "||") {
                if (
                    this.assignability.isAssignable(leftType, this.primitiveTypes.boolean) &&
                    this.assignability.isAssignable(rightType, this.primitiveTypes.boolean)
                ) {
                    return this.primitiveTypes.boolean;
                }
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: `Operator '${operator}' requires two booleans.`,
                    subProblems: []
                };
            } else if (operator === "==" || operator === "!=") {
                return this.primitiveTypes.boolean;
            } else {
                assertUnreachable(operator);
            }
        });

        this.registerValidationRule(this.types.binaryExpressionType, (node, accept) => {
            const type = this.inference.inferType(node);
            if (!Array.isArray(type)) {
                return;
            }
            const leftType = this.inference.inferType(node.left);
            const rightType = this.inference.inferType(node.right);
            if (Array.isArray(leftType) || Array.isArray(rightType)) {
                return;
            }
            accept({
                $problem: this.validationProblem,
                severity: "error",
                languageNode: node,
                message: `Types '${leftType.getName()}' and '${rightType.getName()}' are not valid for binary operator '${node.operator}'.`,
                subProblems: []
            });
        });
    }

    /**
     * Registers type inference and validation rules for ternary expressions.
     * Handles the conditional operator (condition ? trueExpr : falseExpr).
     */
    private registerTernaryExpressionRules(): void {
        this.registerInferenceRule(this.types.ternaryExpressionType, (node) => {
            const trueType = this.inference.inferType(node.trueExpression);
            const falseType = this.inference.inferType(node.falseExpression);
            if (
                !this.typir.factory.CustomValues.isCustomValueType(trueType) ||
                !this.typir.factory.CustomValues.isCustomValueType(falseType)
            ) {
                return this.primitiveTypes.Any;
            }
            const commonType = findCommonParentType(trueType, falseType, this.typir);
            if (commonType != undefined) {
                return commonType;
            } else {
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: `No common parent type found between '${trueType.getName()}' and '${falseType.getName()}'.`,
                    subProblems: []
                };
            }
        });

        this.registerValidationRule(this.types.ternaryExpressionType, (node, accept) => {
            const conditionType = this.inference.inferType(node.condition);
            if (Array.isArray(conditionType)) {
                return;
            }
            const expectedConditionType = this.primitiveTypes.boolean;
            if (!this.typir.Assignability.isAssignable(conditionType, expectedConditionType)) {
                accept({
                    $problem: this.validationProblem,
                    severity: "error",
                    languageNode: node,
                    message: `Condition type '${conditionType.getName()}' is not a boolean'.`,
                    subProblems: []
                });
            }
        });
    }

    /**
     * Helper function to check if a type is a number type (assignable to double).
     *
     * @param type The type to check
     * @returns True if the type is a number type, false otherwise
     */
    private isNumberType(type: CustomValueType): boolean {
        return this.assignability.isAssignable(type, this.primitiveTypes.double);
    }

    /**
     * Finds the closest assignable type between two number types.
     * Checks assignability in both directions and returns the one that works.
     *
     * @param left The left operand type
     * @param right The right operand type
     * @returns The closest assignable type
     * @throws Error if neither type is assignable to the other
     */
    private findClosestNumberType(left: CustomValueType, right: CustomValueType): CustomValueType {
        if (this.assignability.isAssignable(left, right)) {
            return right;
        } else if (this.assignability.isAssignable(right, left)) {
            return left;
        } else {
            throw new Error(
                `No assignable relationship found between number types '${left.getName()}' and '${right.getName()}'.`
            );
        }
    }
}
