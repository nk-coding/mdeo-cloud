import {
    createRule,
    ID,
    manySep,
    LeadingTrailing,
    optional,
    generateImportRules,
    newlineSep,
    NewlineSepSectionCardinality
} from "@mdeo/language-common";
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
    scriptFileScopingConfig,
    ReturnStatement
} from "./types.js";

const { typeRule: TypeRule, returnTypeRule: ReturnTypeRule } = generateTypeRules(typeConfig, typeTypes);

/**
 * The expression and type rules.
 */
const { expressionRule: ExpressionRule, assignableExpressionRule: AssignableExpressionRule } = generateExpressionRules(
    expressionConfig,
    expressionTypes,
    TypeRule,
    []
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
    .as(({ add }) => [
        newlineSep([
            {
                entry: add("imports", FunctionFileImportRule),
                cardinality: NewlineSepSectionCardinality.MANY
            },
            {
                entry: add("functions", FunctionRule),
                cardinality: NewlineSepSectionCardinality.MANY
            }
        ])
    ]);
