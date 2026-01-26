/**
 * Base interface for all statements.
 */
export interface TypedStatement {
    /**
     * The kind of statement.
     */
    kind: string;
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
    kind: "if";
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
    kind: "while";
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
    kind: "for";
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
    kind: "variableDeclaration";
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
    kind: "assignment";
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
    kind: "expression";
    /**
     * The expression to evaluate.
     */
    expression: TypedExpression;
}

/**
 * Break statement.
 */
export interface TypedBreakStatement extends TypedStatement {
    kind: "break";
}

/**
 * Continue statement.
 */
export interface TypedContinueStatement extends TypedStatement {
    kind: "continue";
}

/**
 * Return statement.
 */
export interface TypedReturnStatement extends TypedStatement {
    kind: "return";
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
    kind: string;

    /**
     * Index into the types array for the type this expression evaluates to.
     */
    evalType: number;
}

/**
 * Unary expression.
 */
export interface TypedUnaryExpression extends TypedExpression {
    kind: "unary";
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
    kind: "binary";
    /**
     * The binary operator being applied.
     */
    operator: "+" | "-" | "*" | "/" | "%" | "??" | "&&" | "||" | "==" | "!=" | "===" | "!==" | "<" | ">" | "<=" | ">=";
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
    kind: "ternary";
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
 * Assert non-null expression (!! postfix operator).
 * Asserts that the expression is not null, throwing a NullPointerException if it is.
 */
export interface TypedAssertNonNullExpression extends TypedExpression {
    kind: "assertNonNull";
    /**
     * The expression to assert as non-null.
     */
    expression: TypedExpression;
}

/**
 * Type cast expression (as / as?).
 * Casts the expression to the specified target type.
 */
export interface TypedTypeCastExpression extends TypedExpression {
    kind: "typeCast";
    /**
     * The expression to cast.
     */
    expression: TypedExpression;
    /**
     * Index into the types array for the target type to cast to.
     */
    targetType: number;
    /**
     * Whether this is a safe cast (as?) that returns null instead of throwing.
     */
    isSafe: boolean;
}

/**
 * Type check expression (is / !is).
 * Checks whether the expression is of the specified type.
 */
export interface TypedTypeCheckExpression extends TypedExpression {
    kind: "typeCheck";
    /**
     * The expression to check.
     */
    expression: TypedExpression;
    /**
     * Index into the types array for the type to check against.
     */
    checkType: number;
    /**
     * Whether this is a negated check (!is).
     */
    isNegated: boolean;
}

/**
 * Member access expression.
 */
export interface TypedMemberAccessExpression extends TypedExpression {
    kind: "memberAccess";
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
    kind: "identifier";
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
    kind: "stringLiteral";
    /**
     * The string value.
     */
    value: string;
}

/**
 * Int literal expression.
 */
export interface TypedIntLiteralExpression extends TypedExpression {
    kind: "intLiteral";
    /**
     * The integer value as a string.
     */
    value: string;
}

/**
 * Long literal expression.
 */
export interface TypedLongLiteralExpression extends TypedExpression {
    kind: "longLiteral";
    /**
     * The long value as a string.
     */
    value: string;
}

/**
 * Float literal expression.
 */
export interface TypedFloatLiteralExpression extends TypedExpression {
    kind: "floatLiteral";
    /**
     * The float value as a string.
     */
    value: string;
}

/**
 * Double literal expression.
 */
export interface TypedDoubleLiteralExpression extends TypedExpression {
    kind: "doubleLiteral";
    /**
     * The double value as a string.
     */
    value: string;
}

/**
 * Boolean literal expression.
 */
export interface TypedBooleanLiteralExpression extends TypedExpression {
    kind: "booleanLiteral";
    /**
     * The boolean value.
     */
    value: boolean;
}

/**
 * Null literal expression.
 */
export interface TypedNullLiteralExpression extends TypedExpression {
    kind: "nullLiteral";
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
    kind: "call";
    /**
     * The expression being called (function or lambda).
     */
    expression: TypedExpression;
}

/**
 * Function call expression.
 */
export interface TypedFunctionCallExpression extends TypedCallExpression {
    kind: "functionCall";
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
    kind: "memberCall";
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
    kind: "extensionCall";
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
