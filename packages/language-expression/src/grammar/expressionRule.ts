import {
    action,
    atLeastOneSep,
    createInfixRule,
    createRule,
    createTerminal,
    FLOAT,
    group,
    ID,
    INT,
    LeadingTrailing,
    manySep,
    optional,
    or,
    STRING,
    type ParserRule
} from "@mdeo/language-common";
import type { ExpressionConfig } from "./expressionConfig.js";
import type { BaseExpressionType, ExpressionTypes } from "./expressionTypes.js";
import type { BaseTypeType } from "./typeTypes.js";

/**
 * Generates expression-related parser rules based on the provided configuration.
 *
 * This function creates a complete set of parser rules for common programming language
 * expressions including literals (string, number, boolean), identifiers, member access,
 * function calls with generic arguments, unary operations, binary operations with proper
 * precedence, and ternary conditional expressions. The generated rules follow standard
 * operator precedence and support extensibility through additional custom expression rules.
 *
 * @param config Configuration object containing naming for all expression rules and types
 * @param types The generated expression type interfaces to use as return types
 * @param typeRule The type parser rule to use for generic type arguments
 * @param additionalExpressionRules Optional array of custom expression rules to include
 * @returns The top-level expression parser rule
 */
export function generateExpressionRules(
    config: ExpressionConfig,
    types: ExpressionTypes,
    typeRule: ParserRule<BaseTypeType>,
    additionalExpressionRules: ParserRule<BaseExpressionType>[]
): ParserRule<BaseExpressionType> {
    const stringLiteralExpressionRule = createRule(config.stringLiteralExpressionRuleName)
        .returns(types.stringLiteralExpressionType)
        .as(({ set }) => [set("value", STRING)]);

    const numberLiteralExpressionRule = createRule(config.numberLiteralExpressionRuleName)
        .returns(types.numberLiteralExpressionType)
        .as(({ set }) => [or(set("value", INT), set("value", FLOAT))]);

    const BOOLEAN = createTerminal("BOOLEAN")
        .returns(Boolean)
        .as(/true|false/);

    const booleanLiteralExpressionRule = createRule(config.booleanLiteralExpressionRuleName)
        .returns(types.booleanLiteralExpressionType)
        .as(({ set }) => [set("value", BOOLEAN)]);

    const identifierExpressionRule = createRule(config.identifierExpressionRuleName)
        .returns(types.identifierExpressionType)
        .as(({ set }) => [set("name", ID)]);

    const primaryExpressionRule = createRule(config.primaryExpressionRuleName)
        .returns(types.baseExpressionType)
        .as(() => [
            or(
                stringLiteralExpressionRule,
                numberLiteralExpressionRule,
                booleanLiteralExpressionRule,
                identifierExpressionRule,
                group("(", () => expressionRule, ")"),
                ...additionalExpressionRules
            )
        ]);

    const memberAccessExpressionRule = createRule(config.memberAccessExpressionRuleName)
        .returns(types.baseExpressionType)
        .as(() => [
            or(
                primaryExpressionRule,
                action(types.memberAccessExpressionType, ({ set }) => [
                    set("expression", primaryExpressionRule),
                    ".",
                    set("member", ID)
                ])
            )
        ]);

    const callExpressionGenericArgsRule = createRule(config.callExpressionGenericArgsRuleName)
        .returns(types.callExpressionGenericArgsType)
        .as(({ add }) => [
            optional("<", ...atLeastOneSep(add("typeArguments", typeRule), ",", LeadingTrailing.TRAILING), ">")
        ]);

    const callExpressionRule = createRule(config.callExpressionRuleName)
        .returns(types.baseExpressionType)
        .as(() => [
            or(
                memberAccessExpressionRule,
                action(types.callExpressionType, ({ set, add }) => [
                    set("expression", memberAccessExpressionRule),
                    set("genericArgs", callExpressionGenericArgsRule),
                    "(",
                    ...manySep(
                        add("arguments", () => expressionRule),
                        ",",
                        LeadingTrailing.TRAILING
                    ),
                    ")"
                ])
            )
        ]);

    const unaryExpressionRule = createRule(config.unaryExpressionRuleName)
        .returns(types.baseExpressionType)
        .as(() => [
            or(
                callExpressionRule,
                action(types.unaryExpressionType, ({ set }) => [
                    set("operator", "!", "-"),
                    set("expression", callExpressionRule)
                ])
            )
        ]);

    const binaryExpressionRule = createInfixRule(config.binaryExpressionRuleName)
        .on(unaryExpressionRule)
        .returns(types.binaryExpressionType)
        .operators("*", "/", "%")
        .operators("+", "-")
        .operators("<", ">", "<=", ">=")
        .operators("==", "!=")
        .operators("&&")
        .operators("||")
        .build();

    const ternaryExpressionRule = createRule(config.ternaryExpressionRuleName)
        .returns(types.ternaryExpressionType)
        .as(() => [
            or(
                binaryExpressionRule,
                action(types.ternaryExpressionType, ({ set }) => [
                    set("condition", binaryExpressionRule),
                    "?",
                    set("trueExpression", binaryExpressionRule),
                    ":",
                    set("falseExpression", binaryExpressionRule)
                ])
            )
        ]);

    const expressionRule = createRule(config.expressionRuleName)
        .returns(types.baseExpressionType)
        .as(() => [ternaryExpressionRule]);

    return expressionRule;
}
