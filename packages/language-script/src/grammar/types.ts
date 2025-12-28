import {
    createInterface,
    type ASTType,
    FileScopingConfig,
    generateImportTypes,
    Optional,
    createType
} from "@mdeo/language-common";
import {
    ExpressionConfig,
    generateExpressionTypes,
    TypeConfig,
    generateTypeTypes,
    StatementConfig,
    generateStatementTypes
} from "@mdeo/language-expression";

/**
 * Configuration for the Script language types.
 */
export const typeConfig = new TypeConfig("Script");

/**
 * Generated type types for the Script language.
 */
export const typeTypes = generateTypeTypes(typeConfig);

/**
 * Configuration for the Script language expressions.
 */
export const expressionConfig = new ExpressionConfig("Script");

/**
 * Generated expression types for the Script language.
 */
export const expressionTypes = generateExpressionTypes(expressionConfig, typeTypes);

/**
 * Configuration for the Script language statements.
 */
export const statementConfig = new StatementConfig("Script");

/**
 * Generated statement types for the Script language.
 */
const baseStatementTypes = generateStatementTypes(statementConfig, expressionTypes);

/**
 * Return statement type for Script functions.
 */
export const ReturnStatement = createInterface("ScriptReturnStatement")
    .extends(baseStatementTypes.baseStatementType)
    .attrs({
        value: Optional(expressionTypes.baseExpressionType)
    });

/**
 * Type representing a ReturnStatement AST node.
 */
export type ReturnStatementType = ASTType<typeof ReturnStatement>;

/**
 * All statement types including Script-specific statements.
 */
export const statementTypes = {
    ...baseStatementTypes,
    returnStatementType: ReturnStatement
};

/**
 * Type representing the base expression type.
 */
export const BaseExpression = expressionTypes.baseExpressionType;

/**
 * Type representing the base statement type.
 */
export const BaseStatement = statementTypes.baseStatementType;

/**
 * Type representing the statements scope type.
 */
export const StatementsScope = statementTypes.statementsScopeType;

/**
 * Lambda parameter type.
 */
export const LambdaParameter = createInterface("ScriptLambdaParameter").attrs({
    name: String
});

/**
 * Lambda expression type for Script.
 */
export const LambdaExpression = createInterface("ScriptLambdaExpression")
    .extends(BaseExpression)
    .attrs({
        parameters: [LambdaParameter],
        expression: Optional(BaseExpression),
        body: Optional(StatementsScope)
    });

/**
 * Type representing a LambdaExpression AST node.
 */
export type LambdaExpressionType = ASTType<typeof LambdaExpression>;

/**
 * Type representing a LambdaParameter AST node.
 */
export type LambdaParameterType = ASTType<typeof LambdaParameter>;

/**
 * Type representing the base type annotation.
 */
export const ScriptBaseType = typeTypes.baseTypeType;

/**
 * Parameter type for functions.
 */
export const FunctionParameter = createInterface("ScriptFunctionParameter").attrs({
    name: String,
    type: ScriptBaseType
});

/**
 * Function type.
 */
export const Function = createInterface("ScriptFunction").attrs({
    name: String,
    parameters: [FunctionParameter],
    returnType: Optional(typeTypes.returnTypeType),
    body: StatementsScope
});

/**
 * Type representing a Function AST node.
 */
export type FunctionType = ASTType<typeof Function>;

/**
 * File scoping configuration for functions.
 */
export const scriptFileScopingConfig = new FileScopingConfig<FunctionType>("ScriptFunction", Function);

/**
 * Import types for functions.
 */
export const { importType: FunctionImport, fileImportType: FunctionFileImport } =
    generateImportTypes(scriptFileScopingConfig);

/**
 * Union type for Function or FunctionImport.
 */
export const FunctionOrImport = createType("ScriptFunctionOrImport").types(Function, FunctionImport);

/**
 * Type representing FunctionOrImport AST node.
 */
export type FunctionOrImportType = ASTType<typeof FunctionOrImport>;

/**
 * The root Script type.
 */
export const Script = createInterface("Script").attrs({
    imports: [FunctionFileImport],
    functions: [Function]
});

/**
 * Type representing the Script AST node.
 */
export type ScriptType = ASTType<typeof Script>;
