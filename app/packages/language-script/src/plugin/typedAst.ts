import type { ReturnType } from "@mdeo/language-expression";

/**
 * Enum for all statement types in the TypedAST.
 */
export enum TypedStatementKind {
    If = "if",
    While = "while",
    For = "for",
    VariableDeclaration = "variableDeclaration",
    Assignment = "assignment",
    Expression = "expression",
    Break = "break",
    Continue = "continue",
    Return = "return"
}

/**
 * Enum for all expression types in the TypedAST.
 */
export enum TypedExpressionKind {
    Unary = "unary",
    Binary = "binary",
    Ternary = "ternary",
    ExpressionCall = "call",
    FunctionCall = "functionCall",
    MemberCall = "memberCall",
    ExtensionCall = "extensionCall",
    MemberAccess = "memberAccess",
    Identifier = "identifier",
    StringLiteral = "stringLiteral",
    IntLiteral = "intLiteral",
    LongLiteral = "longLiteral",
    FloatLiteral = "floatLiteral",
    DoubleLiteral = "doubleLiteral",
    BooleanLiteral = "booleanLiteral",
    NullLiteral = "nullLiteral",
    Lambda = "lambda"
}

/**
 * Root of the TypedAST containing all program information.
 */
export interface TypedAst {
    /**
     * Array of all types used in the program.
     * Generics are replaced by Any? due to type erasure.
     * Indexed by typeIndex in expressions.
     */
    types: ReturnType[];

    /**
     * All imports in the program.
     */
    imports: TypedImport[];

    /**
     * All top-level functions in the program.
     */
    functions: TypedFunction[];
}

/**
 * Import declaration.
 */
export interface TypedImport {
    /**
     * Name under which it is registered in the global scope.
     */
    name: string;

    /**
     * The name of the function in its source file.
     */
    ref: string;

    /**
     * URI from where it is imported.
     */
    uri: string;
}

/**
 * Function declaration.
 */
export interface TypedFunction {
    /**
     * Name of the function.
     */
    name: string;

    /**
     * Parameters of the function.
     */
    parameters: TypedParameter[];

    /**
     * Index into the types array for the return type.
     */
    returnType: number;

    /**
     * Body of the function.
     */
    body: TypedCallableBody;
}

/**
 * Function or lambda parameter.
 */
export interface TypedParameter {
    /**
     * Name of the parameter.
     */
    name: string;

    /**
     * Index into the types array for the parameter type.
     */
    type: number;
}

/**
 * Base interface for all statements.
 */
export interface TypedStatement {
    /**
     * The kind of statement.
     */
    kind: TypedStatementKind;
}

/**
 * Body of a callable (function or lambda).
 */
export interface TypedCallableBody {
    /**
     * Statements in the body of the callable.
     */
    body: TypedStatement[];
}

/**
 * If statement.
 */
export interface TypedIfStatement extends TypedStatement {
    kind: TypedStatementKind.If;
    /**
     * The condition expression to evaluate.
     */
    condition: TypedExpression;
    /**
     * Statements to execute if the condition is true.
     */
    thenBlock: TypedStatement[];
    /**
     * Array of else-if clauses.
     */
    elseIfs: TypedElseIfClause[];
    /**
     * Optional else block statements.
     */
    elseBlock?: TypedStatement[];
}

/**
 * Else-if clause.
 */
export interface TypedElseIfClause {
    /**
     * The condition expression to evaluate.
     */
    condition: TypedExpression;
    /**
     * Statements to execute if the condition is true.
     */
    thenBlock: TypedStatement[];
}

/**
 * While statement.
 */
export interface TypedWhileStatement extends TypedStatement {
    kind: TypedStatementKind.While;
    /**
     * The condition expression to evaluate.
     */
    condition: TypedExpression;
    /**
     * Statements to execute while the condition is true.
     */
    body: TypedStatement[];
}

/**
 * For statement.
 */
export interface TypedForStatement extends TypedStatement {
    kind: TypedStatementKind.For;
    /**
     * Name of the loop variable.
     */
    variableName: string;
    /**
     * Index into the types array for the loop variable type.
     */
    variableType: number;
    /**
     * Expression providing the iterable collection.
     */
    iterable: TypedExpression;
    /**
     * Statements to execute in each iteration.
     */
    body: TypedStatement[];
}

/**
 * Variable declaration statement.
 */
export interface TypedVariableDeclarationStatement extends TypedStatement {
    kind: TypedStatementKind.VariableDeclaration;
    /**
     * Name of the variable being declared.
     */
    name: string;
    /**
     * Index into the types array for the variable type.
     */
    type: number;
    /**
     * Optional initial value expression.
     */
    initialValue?: TypedExpression;
}

/**
 * Assignment statement.
 */
export interface TypedAssignmentStatement extends TypedStatement {
    kind: TypedStatementKind.Assignment;
    /**
     * The left-hand side target of the assignment (identifier or member access).
     */
    left: TypedIdentifierExpression | TypedMemberAccessExpression;
    /**
     * The right-hand side value expression to assign.
     */
    right: TypedExpression;
}

/**
 * Expression statement.
 */
export interface TypedExpressionStatement extends TypedStatement {
    kind: TypedStatementKind.Expression;
    /**
     * The expression to evaluate.
     */
    expression: TypedExpression;
}

/**
 * Break statement.
 */
export interface TypedBreakStatement extends TypedStatement {
    kind: TypedStatementKind.Break;
}

/**
 * Continue statement.
 */
export interface TypedContinueStatement extends TypedStatement {
    kind: TypedStatementKind.Continue;
}

/**
 * Return statement.
 */
export interface TypedReturnStatement extends TypedStatement {
    kind: TypedStatementKind.Return;
    /**
     * Optional expression to return.
     */
    value?: TypedExpression;
}

/**
 * Base interface for all expressions.
 */
export interface TypedExpression {
    /**
     * The kind of expression.
     */
    kind: TypedExpressionKind;

    /**
     * Index into the types array for the type this expression evaluates to.
     */
    evalType: number;
}

/**
 * Unary expression.
 */
export interface TypedUnaryExpression extends TypedExpression {
    kind: TypedExpressionKind.Unary;
    /**
     * The unary operator being applied.
     */
    operator: "-" | "!";
    /**
     * The operand expression.
     */
    expression: TypedExpression;
}

/**
 * Binary expression.
 */
export interface TypedBinaryExpression extends TypedExpression {
    kind: TypedExpressionKind.Binary;
    /**
     * The binary operator being applied.
     */
    operator: "+" | "-" | "*" | "/" | "%" | "&&" | "||" | "==" | "!=" | "<" | ">" | "<=" | ">=";
    /**
     * The left operand expression.
     */
    left: TypedExpression;
    /**
     * The right operand expression.
     */
    right: TypedExpression;
}

/**
 * Ternary expression.
 */
export interface TypedTernaryExpression extends TypedExpression {
    kind: TypedExpressionKind.Ternary;
    /**
     * The condition expression to evaluate.
     */
    condition: TypedExpression;
    /**
     * Expression to evaluate if condition is true.
     */
    trueExpression: TypedExpression;
    /**
     * Expression to evaluate if condition is false.
     */
    falseExpression: TypedExpression;
}

/**
 * Base interface for call expressions.
 */
export interface TypedCallExpression extends TypedExpression {
    /**
     * Array of argument expressions.
     */
    arguments: TypedExpression[];
}

/**
 * Expression call expression.
 */
export interface TypedExpressionCallExpression extends TypedCallExpression {
    kind: TypedExpressionKind.ExpressionCall;
    /**
     * The expression being called (function or lambda).
     */
    expression: TypedExpression;
}

/**
 * Function call expression.
 */
export interface TypedFunctionCallExpression extends TypedCallExpression {
    kind: TypedExpressionKind.FunctionCall;
    /**
     * The name of the function being called.
     */
    name: string;
    /**
     * Overload identifier for the function being called.
     */
    overload: string;
}

/**
 * Member call expression.
 */
export interface TypedMemberCallExpression extends TypedCallExpression {
    kind: TypedExpressionKind.MemberCall;
    /**
     * The expression on which the member function is called.
     */
    expression: TypedExpression;
    /**
     * The name of the member function being called.
     */
    member: string;
    /**
     * Whether this uses null-safe chaining (?.).
     */
    isNullChaining: boolean;
    /**
     * Overload identifier for the member function being called.
     */
    overload: string;
}

/**
 * Argument for an extension call.
 */
export interface TypedExtensionCallArgument {
    /**
     * Name of the argument.
     */
    name: string;
    /**
     * The value expression of the argument.
     */
    value: TypedExpression;
}

/**
 * Extension call expression.
 * Represents a virtual function call for a custom extension expression
 */
export interface TypedExtensionCallExpression extends TypedExpression {
    kind: TypedExpressionKind.ExtensionCall;
    /**
     * The name of the function being called
     */
    name: string;
    /**
     * Array of named arguments.
     * Caution: order matters for extension calls!
     * The same name can appear multiple times, it's the responsibility of the interpreter
     * to resolve them in the correct order, and then dynamically create the lists for passing to the function as needed
     */
    arguments: TypedExtensionCallArgument[];
    /**
     * Overload identifier for the extension function being called.
     */
    overload: string;
}

/**
 * Member access expression.
 */
export interface TypedMemberAccessExpression extends TypedExpression {
    kind: TypedExpressionKind.MemberAccess;
    /**
     * The expression on which the member is accessed.
     */
    expression: TypedExpression;
    /**
     * The name of the member being accessed.
     */
    member: string;
    /**
     * Whether this uses null-safe chaining (?.).
     */
    isNullChaining: boolean;
}

/**
 * Identifier expression.
 */
export interface TypedIdentifierExpression extends TypedExpression {
    kind: TypedExpressionKind.Identifier;
    /**
     * The name of the identifier.
     */
    name: string;
    /**
     * Scope level where this identifier is defined.
     * 0 = global scope, increases with nesting.
     */
    scope: number;
}

/**
 * String literal expression.
 */
export interface TypedStringLiteralExpression extends TypedExpression {
    kind: TypedExpressionKind.StringLiteral;
    /**
     * The string value.
     */
    value: string;
}

/**
 * Int literal expression.
 */
export interface TypedIntLiteralExpression extends TypedExpression {
    kind: TypedExpressionKind.IntLiteral;
    /**
     * The integer value as a string.
     */
    value: string;
}

/**
 * Long literal expression.
 */
export interface TypedLongLiteralExpression extends TypedExpression {
    kind: TypedExpressionKind.LongLiteral;
    /**
     * The long value as a string.
     */
    value: string;
}

/**
 * Float literal expression.
 */
export interface TypedFloatLiteralExpression extends TypedExpression {
    kind: TypedExpressionKind.FloatLiteral;
    /**
     * The float value as a string.
     */
    value: string;
}

/**
 * Double literal expression.
 */
export interface TypedDoubleLiteralExpression extends TypedExpression {
    kind: TypedExpressionKind.DoubleLiteral;
    /**
     * The double value as a string.
     */
    value: string;
}

/**
 * Boolean literal expression.
 */
export interface TypedBooleanLiteralExpression extends TypedExpression {
    kind: TypedExpressionKind.BooleanLiteral;
    /**
     * The boolean value.
     */
    value: boolean;
}

/**
 * Null literal expression.
 */
export interface TypedNullLiteralExpression extends TypedExpression {
    kind: TypedExpressionKind.NullLiteral;
}

/**
 * Lambda expression.
 */
export interface TypedLambdaExpression extends TypedExpression {
    kind: TypedExpressionKind.Lambda;
    /**
     * Parameters of the lambda (names only, types are in the evalType).
     */
    parameters: string[];
    /**
     * Body of the lambda as statements.
     */
    body: TypedCallableBody;
}

/**
 * Union type for all statement types.
 */
export type TypedStatementUnion =
    | TypedIfStatement
    | TypedWhileStatement
    | TypedForStatement
    | TypedVariableDeclarationStatement
    | TypedAssignmentStatement
    | TypedExpressionStatement
    | TypedBreakStatement
    | TypedContinueStatement
    | TypedReturnStatement;

/**
 * Union type for all expression types.
 */
export type TypedExpressionUnion =
    | TypedUnaryExpression
    | TypedBinaryExpression
    | TypedTernaryExpression
    | TypedCallExpression
    | TypedMemberAccessExpression
    | TypedIdentifierExpression
    | TypedStringLiteralExpression
    | TypedIntLiteralExpression
    | TypedLongLiteralExpression
    | TypedFloatLiteralExpression
    | TypedDoubleLiteralExpression
    | TypedBooleanLiteralExpression
    | TypedNullLiteralExpression
    | TypedLambdaExpression;
