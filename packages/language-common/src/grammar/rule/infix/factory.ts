import { InfixRuleBuilder } from "./builders.js";

/**
 * Creates a new infix rule builder for defining infix operators in the grammar.
 *
 * Infix rules allow you to define binary operators with precedence and associativity,
 * such as arithmetic operators (+, -, *, /) or comparison operators (==, !=, <, >).
 *
 * @param name The unique name for this infix rule in the grammar
 * @returns A new InfixRuleBuilder instance for configuring the infix rule
 *
 * @example
 * ```typescript
 * // Create an infix rule for arithmetic expressions
 * const ExpressionRule = createInfixRule("ExpressionRule")
 *     .on(SimpleExpressionRule)
 *     .returns(BinaryExpression)
 *     .operators("*", "/")        // Higher precedence
 *     .operators("+", "-");       // Lower precedence
 * ```
 */
export function createInfixRule(name: string): InfixRuleBuilder {
    return new InfixRuleBuilder(name);
}
