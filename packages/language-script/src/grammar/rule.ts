import { createRule, ID, many, manySep, LeadingTrailing, optional, generateImportRules } from "@mdeo/language-common";
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
    Function,
    FunctionImport,
    FunctionFileImport,
    scriptFileScopingConfig
} from "./types.js";

const TypeRule = generateTypeRules(typeConfig, typeTypes);

/**
 * The expression and type rules.
 */
const ExpressionRule = generateExpressionRules(expressionConfig, expressionTypes, TypeRule, []);

/**
 * The statement rules.
 */
const { statementsScopeRule: StatementsScopeRule } = generateStatementRules(
    statementConfig,
    statementTypes,
    ExpressionRule,
    TypeRule,
    []
);

/**
 * Function parameter rule.
 */
const FunctionParameterRule = createRule("ScriptFunctionParameterRule")
    .returns(FunctionParameter)
    .as(({ set }) => [set("name", ID), optional(":", set("type", TypeRule))]);

/**
 * Function rule.
 */
const FunctionRule = createRule("ScriptFunctionRule")
    .returns(Function)
    .as(({ set, add }) => [
        "fun",
        set("name", ID),
        "(",
        ...manySep(add("parameters", FunctionParameterRule), ",", LeadingTrailing.TRAILING),
        ")",
        optional(":", set("returnType", TypeRule)),
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
    .as(({ add }) => [many(add("imports", FunctionFileImportRule)), many(add("functions", FunctionRule))]);
