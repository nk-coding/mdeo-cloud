import {
    action,
    atLeastOneSep,
    createInfixRule,
    createRule,
    createTerminal,
    group,
    ID,
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
 * Integer token (32-bit) - returns string to preserve precision
 */
const INT_LITERAL = createTerminal("INT_LITERAL")
    .returns(String)
    .as(/[0-9]+/);

/**
 * Long token (64-bit integer, suffixed with L or l) - returns string to preserve precision
 */
const LONG_LITERAL = createTerminal("LONG_LITERAL")
    .returns(String)
    .as(/[0-9]+[Ll]/);

/**
 * Float token (32-bit floating point, suffixed with F or f) - returns string to preserve precision
 */
const FLOAT_LITERAL = createTerminal("FLOAT_LITERAL")
    .returns(String)
    .as(/[0-9]+\.[0-9]+[Ff]/);

/**
 * Double token (64-bit floating point, optionally suffixed with D or d) - returns string to preserve precision
 */
const DOUBLE_LITERAL = createTerminal("DOUBLE_LITERAL")
    .returns(String)
    .as(/[0-9]+\.[0-9]+[Dd]?/);

/**
 * Boolean token
 */
const BOOLEAN = createTerminal("BOOLEAN")
    .returns(Boolean)
    .as(/true|false/);

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
) {
    const stringLiteralExpressionRule = createRule(config.stringLiteralExpressionRuleName)
        .returns(types.stringLiteralExpressionType)
        .as(({ set }) => [set("value", STRING)]);

    const intLiteralExpressionRule = createRule(config.intLiteralExpressionRuleName)
        .returns(types.intLiteralExpressionType)
        .as(({ set }) => [set("value", INT_LITERAL)]);

    const longLiteralExpressionRule = createRule(config.longLiteralExpressionRuleName)
        .returns(types.longLiteralExpressionType)
        .as(({ set }) => [set("value", LONG_LITERAL)]);

    const floatLiteralExpressionRule = createRule(config.floatLiteralExpressionRuleName)
        .returns(types.floatLiteralExpressionType)
        .as(({ set }) => [set("value", FLOAT_LITERAL)]);

    const doubleLiteralExpressionRule = createRule(config.doubleLiteralExpressionRuleName)
        .returns(types.doubleLiteralExpressionType)
        .as(({ set }) => [set("value", DOUBLE_LITERAL)]);

    const booleanLiteralExpressionRule = createRule(config.booleanLiteralExpressionRuleName)
        .returns(types.booleanLiteralExpressionType)
        .as(({ set }) => [set("value", BOOLEAN)]);

    const nullLiteralExpressionRule = createRule(config.nullLiteralExpressionTypeName)
        .returns(types.nullLiteralExpressionType)
        .as(() => ["null"]);

    const identifierExpressionRule = createRule(config.identifierExpressionRuleName)
        .returns(types.identifierExpressionType)
        .as(({ set }) => [set("name", ID)]);

    const primaryExpressionRule = createRule(config.primaryExpressionRuleName)
        .returns(types.baseExpressionType)
        .as(() => [
            or(
                stringLiteralExpressionRule,
                intLiteralExpressionRule,
                longLiteralExpressionRule,
                floatLiteralExpressionRule,
                doubleLiteralExpressionRule,
                booleanLiteralExpressionRule,
                nullLiteralExpressionRule,
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
                action(types.memberAccessExpressionType, ({ set, flag }) => [
                    set("expression", primaryExpressionRule),
                    or(flag("isNullChaining", "?."), "."),
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

    const assignableExpressionRule = createRule(config.assignableExpressionRuleName)
        .returns(types.assignableExpressionType)
        .as(() => [or(identifierExpressionRule, memberAccessExpressionRule)]);

    return {
        expressionRule,
        assignableExpressionRule
    };
}
