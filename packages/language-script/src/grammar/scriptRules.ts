import { createRule, ID, optional, or, NEWLINE, many } from "@mdeo/language-common";
import { generateExpressionRules, generateStatementRules, generateTypeRules } from "@mdeo/language-expression";
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
    LambdaParameters
} from "./scriptTypes.js";
import { generateImportRules, LeadingTrailing, manySep } from "@mdeo/language-shared";

const { typeRule: TypeRule, returnTypeRule: ReturnTypeRule } = generateTypeRules(typeConfig, typeTypes);

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
        or(set("expression", ExpressionRule), set("body", StatementsScopeRule))
    ]);

/**
 * The expression and type rules.
 */
const { expressionRule: ExpressionRule, assignableExpressionRule: AssignableExpressionRule } = generateExpressionRules(
    expressionConfig,
    expressionTypes,
    TypeRule,
    [LambdaExpressionRule]
);

/**
 * Return statement rule.
 */
const ReturnStatementRule = createRule("ScriptReturnStatementRule")
    .returns(ReturnStatement)
    .as(({ set }) => ["return", optional(set("value", ExpressionRule))]);

/**
 * The statement rules.
 */
const { statementsScopeRule: StatementsScopeRule } = generateStatementRules(
    statementConfig,
    statementTypes,
    ExpressionRule,
    AssignableExpressionRule,
    TypeRule,
    [ReturnStatementRule]
);

/**
 * Function parameter rule.
 */
const FunctionParameterRule = createRule("ScriptFunctionParameterRule")
    .returns(FunctionParameter)
    .as(({ set }) => [set("name", ID), optional(":", set("type", TypeRule))]);

/**
 * Function parameters rule (with round brackets).
 */
const FunctionParametersRule = createRule("ScriptFunctionParametersRule")
    .returns(FunctionParameters)
    .as(({ add }) => ["(", ...manySep(add("parameters", FunctionParameterRule), ",", LeadingTrailing.TRAILING), ")"]);

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
const { importRule: FunctionImportRule, fileImportRule: FunctionFileImportRule } = generateImportRules(
    scriptFileScopingConfig,
    FunctionImport,
    FunctionFileImport,
    ID
);

/**
 * The Script entry rule.
 */
export const ScriptRule = createRule("ScriptRule")
    .returns(Script)
    .as(({ add }) => [many(or(add("imports", FunctionFileImportRule), add("functions", FunctionRule), NEWLINE))]);
