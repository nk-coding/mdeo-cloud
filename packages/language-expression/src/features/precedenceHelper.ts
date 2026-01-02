import type { AstNode } from "langium";
import type { ExpressionTypes, BinaryExpressionType, BaseExpressionType } from "../grammar/expressionTypes.js";

/**
 * Precedence levels for expressions (lower number = higher precedence/binds tighter).
 * Based on the grammar rules in expressionRule.ts:
 * - Primary expressions (literals, identifiers, parenthesized expressions): 0
 * - Member access and call expressions: 1
 * - Unary expressions (!, -): 2
 * - Multiplicative binary operators (*, /, %): 3
 * - Additive binary operators (+, -): 4
 * - Relational binary operators (<, >, <=, >=): 5
 * - Equality binary operators (==, !=): 6
 * - Logical AND (&&): 7
 * - Logical OR (||): 8
 * - Ternary conditional (?:): 9
 */
export const enum Precedence {
    PRIMARY = 0,
    MEMBER_CALL = 1,
    UNARY = 2,
    MULTIPLICATIVE = 3,
    ADDITIVE = 4,
    RELATIONAL = 5,
    EQUALITY = 6,
    LOGICAL_AND = 7,
    LOGICAL_OR = 8,
    TERNARY = 9
}

/**
 * Gets the precedence level of a binary operator.
 *
 * @param operator The binary operator
 * @returns The precedence level
 */
function getBinaryOperatorPrecedence(operator: string): Precedence {
    switch (operator) {
        case "*":
        case "/":
        case "%":
            return Precedence.MULTIPLICATIVE;
        case "+":
        case "-":
            return Precedence.ADDITIVE;
        case "<":
        case ">":
        case "<=":
        case ">=":
            return Precedence.RELATIONAL;
        case "==":
        case "!=":
            return Precedence.EQUALITY;
        case "&&":
            return Precedence.LOGICAL_AND;
        case "||":
            return Precedence.LOGICAL_OR;
        default:
            return Precedence.PRIMARY;
    }
}

/**
 * Determines the precedence level of an expression AST node.
 *
 * @template T The type of the AST node
 * @param node The AST node
 * @param types The generated expression types
 * @returns The precedence level
 */
export function getExpressionPrecedence<T extends BaseExpressionType>(node: T, types: ExpressionTypes): Precedence {
    const nodeType = node.$type;

    if (
        nodeType === types.stringLiteralExpressionType.name ||
        nodeType === types.intLiteralExpressionType.name ||
        nodeType === types.longLiteralExpressionType.name ||
        nodeType === types.floatLiteralExpressionType.name ||
        nodeType === types.doubleLiteralExpressionType.name ||
        nodeType === types.booleanLiteralExpressionType.name ||
        nodeType === types.nullLiteralExpressionType.name ||
        nodeType === types.identifierExpressionType.name
    ) {
        return Precedence.PRIMARY;
    }

    if (nodeType === types.memberAccessExpressionType.name || nodeType === types.callExpressionType.name) {
        return Precedence.MEMBER_CALL;
    }

    if (nodeType === types.unaryExpressionType.name) {
        return Precedence.UNARY;
    }

    if (nodeType === types.binaryExpressionType.name) {
        const binaryNode = node as BinaryExpressionType;
        return getBinaryOperatorPrecedence(binaryNode.operator);
    }

    if (nodeType === types.ternaryExpressionType.name) {
        return Precedence.TERNARY;
    }

    return Precedence.PRIMARY;
}
