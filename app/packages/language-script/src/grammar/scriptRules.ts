import {
    createRule,
    GrammarDeserializationContext,
    ID,
    optional,
    or,
    NEWLINE,
    many,
    HIDDEN_NEWLINE,
    INT,
    FLOAT,
    type ParserRule
} from "@mdeo/language-common";
import {
    generateExpressionRules,
    generateStatementRules,
    generateTypeRules,
    type BaseExpressionType
} from "@mdeo/language-expression";
import {
    expressionConfig,
    expressionTypes,
    typeConfig,
    typeTypes,
    statementConfig,
    statementTypes,
    Script,
    FunctionParameter,
    FunctionParameters,
    Function,
    FunctionImport,
    FunctionFileImport,
    scriptFileScopingConfig,
    ReturnStatement,
    LambdaExpression,
    LambdaParameter,
    LambdaParameters,
    BaseExpression,
    BaseExtension,
    ExtensionExpression,
    type ScriptType
} from "./scriptTypes.js";
import { generateImportRules, LeadingTrailing, manySep } from "@mdeo/language-shared";
import type {
    ResolvedScriptContributionPlugins,
    ScriptContributionPlugin
} from "../plugin/scriptContributionPlugin.js";
import { resolvePlugins } from "../plugin/resolvePlugins.js";

/**
 * Function to generate the Script language root rule.
 *
 * @param plugins The contribution plugins for the Script language.
 * @returns The Script language root rule.
 */
export function generateScriptRule(plugins: ScriptContributionPlugin[]): {
    rule: ParserRule<ScriptType>;
    resolvedPlugins: ResolvedScriptContributionPlugins;
} {
    const { typeRule: TypeRule, returnTypeRule: ReturnTypeRule } = generateTypeRules(typeConfig, typeTypes);

    const additionalExpressionRules: ParserRule<BaseExpressionType>[] = [];

    /**
     * Lambda parameter rule.
     */
    const LambdaParameterRule = createRule("ScriptLambdaParameterRule")
        .returns(LambdaParameter)
        .as(({ set }) => [set("name", ID)]);

    /**
     * Lambda parameters rule (with round brackets).
     */
    const LambdaParametersRule = createRule("ScriptLambdaParametersRule")
        .returns(LambdaParameters)
        .as(({ add }) => ["(", ...manySep(add("parameters", LambdaParameterRule), ",", LeadingTrailing.TRAILING), ")"]);

    /**
     * Lambda expression rule.
     */
    const LambdaExpressionRule = createRule("ScriptLambdaExpressionRule")
        .returns(LambdaExpression)
        .as(({ set }) => [
            set("parameterList", LambdaParametersRule),
            "=>",
            or(set("expression", ExpressionRule), set("body", StatementsScopeRule!))
        ]);

    additionalExpressionRules.push(LambdaExpressionRule);

    /**
     * The expression and type rules.
     */
    const expressionRules = generateExpressionRules(
        expressionConfig,
        expressionTypes,
        TypeRule,
        additionalExpressionRules
    );

    const ExpressionRule = expressionRules.expressionRule;
    const AssignableExpressionRule = expressionRules.assignableExpressionRule;

    const deserializationContext = GrammarDeserializationContext.create(
        [BaseExpression, BaseExtension],
        [...Object.values(expressionRules), LambdaExpressionRule],
        [ID, NEWLINE, HIDDEN_NEWLINE, INT, FLOAT]
    );

    const resolvedPlugins = resolvePlugins(plugins, deserializationContext);

    /**
     * The extension expression rule
     */
    const ExtensionExpressionRule = createRule("ScriptExtensionExpressionRule")
        .returns(ExtensionExpression)
        .as(({ set }) => [or(...resolvedPlugins.rules.map((rule) => set("extension", rule)))]);

    additionalExpressionRules.push(ExtensionExpressionRule);

    /**
     * Return statement rule.
     */
    const ReturnStatementRule = createRule("ScriptReturnStatementRule")
        .returns(ReturnStatement)
        .as(({ set }) => ["return", optional(set("value", ExpressionRule))]);

    /**
     * The statement rules.
     */
    const StatementsScopeRule = generateStatementRules(
        statementConfig,
        statementTypes,
        ExpressionRule,
        AssignableExpressionRule,
        TypeRule,
        [ReturnStatementRule]
    ).statementsScopeRule;

    /**
     * Function parameter rule.
     */
    const FunctionParameterRule = createRule("ScriptFunctionParameterRule")
        .returns(FunctionParameter)
        .as(({ set }) => [set("name", ID), ":", set("type", TypeRule)]);

    /**
     * Function parameters rule (with round brackets).
     */
    const FunctionParametersRule = createRule("ScriptFunctionParametersRule")
        .returns(FunctionParameters)
        .as(({ add }) => [
            "(",
            ...manySep(add("parameters", FunctionParameterRule), ",", LeadingTrailing.TRAILING),
            ")"
        ]);

    /**
     * Function rule.
     */
    const FunctionRule = createRule("ScriptFunctionRule")
        .returns(Function)
        .as(({ set }) => [
            "fun",
            set("name", ID),
            set("parameterList", FunctionParametersRule),
            optional(":", set("returnType", ReturnTypeRule)),
            set("body", StatementsScopeRule)
        ]);

    /**
     * Import rules for functions.
     */
    const { fileImportRule: FunctionFileImportRule } = generateImportRules(
        scriptFileScopingConfig,
        FunctionImport,
        FunctionFileImport,
        ID
    );

    /**
     * The Script entry rule.
     */
    const ScriptRule = createRule("ScriptRule")
        .returns(Script)
        .as(({ add }) => [many(or(add("imports", FunctionFileImportRule), add("functions", FunctionRule), NEWLINE))]);

    return {
        rule: ScriptRule,
        resolvedPlugins
    };
}
