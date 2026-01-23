package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedAst
import com.mdeo.script.ast.TypedCallableBody
import com.mdeo.script.ast.TypedFunction
import com.mdeo.script.ast.TypedImport
import com.mdeo.script.ast.TypedParameter
import com.mdeo.script.ast.expressions.TypedBinaryExpression
import com.mdeo.script.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.script.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionCallExpression
import com.mdeo.script.ast.expressions.TypedFloatLiteralExpression
import com.mdeo.script.ast.expressions.TypedFunctionCallExpression
import com.mdeo.script.ast.expressions.TypedIdentifierExpression
import com.mdeo.script.ast.expressions.TypedIntLiteralExpression
import com.mdeo.script.ast.expressions.TypedLambdaExpression
import com.mdeo.script.ast.expressions.TypedLongLiteralExpression
import com.mdeo.script.ast.expressions.TypedMemberAccessExpression
import com.mdeo.script.ast.expressions.TypedMemberCallExpression
import com.mdeo.script.ast.expressions.TypedNullLiteralExpression
import com.mdeo.script.ast.expressions.TypedStringLiteralExpression
import com.mdeo.script.ast.expressions.TypedTernaryExpression
import com.mdeo.script.ast.expressions.TypedUnaryExpression
import com.mdeo.script.ast.statements.TypedAssignmentStatement
import com.mdeo.script.ast.statements.TypedBreakStatement
import com.mdeo.script.ast.statements.TypedContinueStatement
import com.mdeo.script.ast.statements.TypedElseIfClause
import com.mdeo.script.ast.statements.TypedExpressionStatement
import com.mdeo.script.ast.statements.TypedIfStatement
import com.mdeo.script.ast.statements.TypedForStatement
import com.mdeo.script.ast.statements.TypedReturnStatement
import com.mdeo.script.ast.statements.TypedStatement
import com.mdeo.script.ast.statements.TypedVariableDeclarationStatement
import com.mdeo.script.ast.statements.TypedWhileStatement
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.LambdaType
import com.mdeo.script.ast.types.Parameter
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.VoidType
import com.mdeo.script.runtime.ExecutionEnvironment

/**
 * DSL builder for creating TypedAST structures programmatically.
 * Simplifies test creation by providing a fluent API.
 */
class TypedAstBuilder {
    private val types = mutableListOf<ReturnType>()
    private val functions = mutableListOf<TypedFunction>()
    private val imports = mutableListOf<TypedImport>()
    
    /**
     * Adds a type to the types array and returns its index.
     * 
     * @param type The type to add.
     * @return The index of the type in the types array.
     */
    fun addType(type: ReturnType): Int {
        val existing = types.indexOfFirst { it == type }
        if (existing >= 0) return existing
        types.add(type)
        return types.size - 1
    }
    
    /**
     * Adds the void type and returns its index.
     */
    fun voidType(): Int = addType(VoidType())
    
    /**
     * Adds a non-nullable int type and returns its index.
     */
    fun intType(): Int = addType(ClassTypeRef("builtin.int", false))
    
    /**
     * Adds a nullable int type and returns its index.
     */
    fun intNullableType(): Int = addType(ClassTypeRef("builtin.int", true))
    
    /**
     * Adds a non-nullable long type and returns its index.
     */
    fun longType(): Int = addType(ClassTypeRef("builtin.long", false))
    
    /**
     * Adds a nullable long type and returns its index.
     */
    fun longNullableType(): Int = addType(ClassTypeRef("builtin.long", true))
    
    /**
     * Adds a non-nullable float type and returns its index.
     */
    fun floatType(): Int = addType(ClassTypeRef("builtin.float", false))
    
    /**
     * Adds a nullable float type and returns its index.
     */
    fun floatNullableType(): Int = addType(ClassTypeRef("builtin.float", true))
    
    /**
     * Adds a non-nullable double type and returns its index.
     */
    fun doubleType(): Int = addType(ClassTypeRef("builtin.double", false))
    
    /**
     * Adds a nullable double type and returns its index.
     */
    fun doubleNullableType(): Int = addType(ClassTypeRef("builtin.double", true))
    
    /**
     * Adds a non-nullable boolean type and returns its index.
     */
    fun booleanType(): Int = addType(ClassTypeRef("builtin.boolean", false))
    
    /**
     * Adds a nullable boolean type and returns its index.
     */
    fun booleanNullableType(): Int = addType(ClassTypeRef("builtin.boolean", true))
    
    /**
     * Adds a non-nullable string type and returns its index.
     */
    fun stringType(): Int = addType(ClassTypeRef("builtin.string", false))
    
    /**
     * Adds a nullable string type and returns its index.
     */
    fun stringNullableType(): Int = addType(ClassTypeRef("builtin.string", true))
    
    /**
     * Adds a nullable Any type and returns its index.
     */
    fun anyNullableType(): Int = addType(ClassTypeRef("builtin.any", true))
    
    /**
     * Adds a non-nullable list type and returns its index.
     * Uses builtin.list which maps to java.util.List at runtime.
     */
    fun listType(): Int = addType(ClassTypeRef("builtin.list", false))
    
    /**
     * Adds a nullable list type and returns its index.
     */
    fun listNullableType(): Int = addType(ClassTypeRef("builtin.list", true))
    
    /**
     * Adds a lambda type and returns its index.
     * 
     * @param returnType The return type of the lambda.
     * @param parameters The parameters of the lambda as pairs of (name, type).
     * @return The index of the lambda type in the types array.
     */
    fun lambdaType(returnType: ReturnType, vararg parameters: Pair<String, ReturnType>): Int {
        val paramList = parameters.map { (name, type) -> Parameter(name, type as com.mdeo.script.ast.types.ValueType) }
        return addType(LambdaType(returnType, paramList, false))
    }
    
    /**
     * Adds an import to the AST.
     * 
     * @param name The local name for the imported function.
     * @param ref The original function name in the source file.
     * @param uri The URI of the source file.
     */
    fun import(name: String, ref: String, uri: String) {
        imports.add(TypedImport(name = name, ref = ref, uri = uri))
    }
    
    /**
     * Adds a function to the AST.
     * 
     * @param name The function name.
     * @param returnType The index of the return type.
     * @param parameters The function parameters.
     * @param body The function body statements.
     */
    fun function(
        name: String,
        returnType: Int,
        parameters: List<TypedParameter> = emptyList(),
        body: List<TypedStatement>
    ) {
        functions.add(
            TypedFunction(
                name = name,
                parameters = parameters,
                returnType = returnType,
                body = TypedCallableBody(body)
            )
        )
    }
    
    /**
     * Builds the TypedAST.
     */
    fun build(): TypedAst {
        return TypedAst(
            types = types.toList(),
            imports = imports.toList(),
            functions = functions.toList()
        )
    }
}

/**
 * Creates a TypedAST using the builder DSL.
 */
fun buildTypedAst(block: TypedAstBuilder.() -> Unit): TypedAst {
    val builder = TypedAstBuilder()
    builder.block()
    return builder.build()
}

/**
 * Creates an int literal expression.
 */
fun intLiteral(value: Int, typeIndex: Int): TypedIntLiteralExpression {
    return TypedIntLiteralExpression(evalType = typeIndex, value = value.toString())
}

/**
 * Creates a long literal expression.
 */
fun longLiteral(value: Long, typeIndex: Int): TypedLongLiteralExpression {
    return TypedLongLiteralExpression(evalType = typeIndex, value = value.toString())
}

/**
 * Creates a float literal expression.
 */
fun floatLiteral(value: Float, typeIndex: Int): TypedFloatLiteralExpression {
    return TypedFloatLiteralExpression(evalType = typeIndex, value = value.toString())
}

/**
 * Creates a double literal expression.
 */
fun doubleLiteral(value: Double, typeIndex: Int): TypedDoubleLiteralExpression {
    return TypedDoubleLiteralExpression(evalType = typeIndex, value = value.toString())
}

/**
 * Creates a boolean literal expression.
 */
fun booleanLiteral(value: Boolean, typeIndex: Int): TypedBooleanLiteralExpression {
    return TypedBooleanLiteralExpression(evalType = typeIndex, value = value)
}

/**
 * Creates a string literal expression.
 */
fun stringLiteral(value: String, typeIndex: Int): TypedStringLiteralExpression {
    return TypedStringLiteralExpression(evalType = typeIndex, value = value)
}

/**
 * Creates a null literal expression.
 */
fun nullLiteral(typeIndex: Int): TypedNullLiteralExpression {
    return TypedNullLiteralExpression(evalType = typeIndex)
}

/**
 * Creates a binary expression.
 * 
 * @param left The left operand expression.
 * @param operator The binary operator (+, -, *, /, %, <, >, <=, >=, ==, !=, &&, ||).
 * @param right The right operand expression.
 * @param resultTypeIndex The index of the result type in the types array.
 * @return The binary expression.
 */
fun binaryExpr(
    left: TypedExpression,
    operator: String,
    right: TypedExpression,
    resultTypeIndex: Int
): TypedBinaryExpression {
    return TypedBinaryExpression(
        evalType = resultTypeIndex,
        operator = operator,
        left = left,
        right = right
    )
}

/**
 * Creates a unary expression.
 * 
 * @param operator The unary operator (- or !).
 * @param expression The operand expression.
 * @param resultTypeIndex The index of the result type in the types array.
 * @return The unary expression.
 */
fun unaryExpr(
    operator: String,
    expression: TypedExpression,
    resultTypeIndex: Int
): TypedUnaryExpression {
    return TypedUnaryExpression(
        evalType = resultTypeIndex,
        operator = operator,
        expression = expression
    )
}

/**
 * Creates a lambda expression.
 * 
 * @param parameters List of parameter names for the lambda.
 * @param body The statements in the lambda body.
 * @param lambdaTypeIndex The index of the lambda type in the types array.
 * @return The lambda expression.
 */
fun lambdaExpr(
    parameters: List<String>,
    body: List<TypedStatement>,
    lambdaTypeIndex: Int
): TypedLambdaExpression {
    return TypedLambdaExpression(
        evalType = lambdaTypeIndex,
        parameters = parameters,
        body = TypedCallableBody(body)
    )
}

/**
 * Creates a return statement with an expression.
 */
fun returnStmt(expression: TypedExpression): TypedReturnStatement {
    return TypedReturnStatement(value = expression)
}

/**
 * Creates a void return statement.
 */
fun returnVoid(): TypedReturnStatement {
    return TypedReturnStatement(value = null)
}

/**
 * Creates a variable declaration statement.
 * 
 * @param name The variable name.
 * @param typeIndex The type index in the types array.
 * @param initialValue Optional initial value expression.
 * @return The variable declaration statement.
 */
fun varDecl(
    name: String,
    typeIndex: Int,
    initialValue: TypedExpression? = null
): TypedVariableDeclarationStatement {
    return TypedVariableDeclarationStatement(
        name = name,
        type = typeIndex,
        initialValue = initialValue
    )
}

/**
 * Creates an identifier expression.
 * 
 * @param name The variable name.
 * @param typeIndex The type index in the types array.
 * @param scope The scope level where the variable was declared.
 * @return The identifier expression.
 */
fun identifier(
    name: String,
    typeIndex: Int,
    scope: Int
): TypedIdentifierExpression {
    return TypedIdentifierExpression(
        evalType = typeIndex,
        name = name,
        scope = scope
    )
}

/**
 * Creates an assignment statement.
 * 
 * @param left The left-hand side (target) expression.
 * @param right The right-hand side (value) expression.
 * @return The assignment statement.
 */
fun assignment(
    left: TypedExpression,
    right: TypedExpression
): TypedAssignmentStatement {
    return TypedAssignmentStatement(
        left = left,
        right = right
    )
}

/**
 * Creates a while statement.
 * 
 * @param condition The loop condition expression.
 * @param body The statements to execute in the loop body.
 * @return The while statement.
 */
fun whileStmt(
    condition: TypedExpression,
    body: List<TypedStatement>
): TypedWhileStatement {
    return TypedWhileStatement(
        condition = condition,
        body = body
    )
}

/**
 * Creates a for statement.
 * 
 * @param variableName The name of the loop variable.
 * @param variableType The type index of the loop variable.
 * @param iterable The expression providing the iterable collection.
 * @param body The statements to execute in each iteration.
 * @return The for statement.
 */
fun forStmt(
    variableName: String,
    variableType: Int,
    iterable: TypedExpression,
    body: List<TypedStatement>
): TypedForStatement {
    return TypedForStatement(
        variableName = variableName,
        variableType = variableType,
        iterable = iterable,
        body = body
    )
}

/**
 * Creates an if statement.
 * 
 * @param condition The condition expression.
 * @param thenBlock The statements to execute if condition is true.
 * @param elseIfs Optional list of else-if clauses.
 * @param elseBlock Optional else block statements.
 * @return The if statement.
 */
fun ifStmt(
    condition: TypedExpression,
    thenBlock: List<TypedStatement>,
    elseIfs: List<TypedElseIfClause> = emptyList(),
    elseBlock: List<TypedStatement>? = null
): TypedIfStatement {
    return TypedIfStatement(
        condition = condition,
        thenBlock = thenBlock,
        elseIfs = elseIfs,
        elseBlock = elseBlock
    )
}

/**
 * Creates an else-if clause.
 * 
 * @param condition The condition expression.
 * @param thenBlock The statements to execute if condition is true.
 * @return The else-if clause.
 */
fun elseIfClause(
    condition: TypedExpression,
    thenBlock: List<TypedStatement>
): TypedElseIfClause {
    return TypedElseIfClause(
        condition = condition,
        thenBlock = thenBlock
    )
}

/**
 * Creates a ternary expression.
 * 
 * @param condition The condition expression.
 * @param trueExpr Expression to evaluate if condition is true.
 * @param falseExpr Expression to evaluate if condition is false.
 * @param resultTypeIndex The index of the result type in the types array.
 * @return The ternary expression.
 */
fun ternaryExpr(
    condition: TypedExpression,
    trueExpr: TypedExpression,
    falseExpr: TypedExpression,
    resultTypeIndex: Int
): TypedTernaryExpression {
    return TypedTernaryExpression(
        evalType = resultTypeIndex,
        condition = condition,
        trueExpression = trueExpr,
        falseExpression = falseExpr
    )
}

/**
 * Creates a break statement.
 * 
 * @return The break statement.
 */
fun breakStmt(): TypedBreakStatement {
    return TypedBreakStatement()
}

/**
 * Creates a continue statement.
 * 
 * @return The continue statement.
 */
fun continueStmt(): TypedContinueStatement {
    return TypedContinueStatement()
}

/**
 * Creates a function call expression.
 * 
 * @param name The name of the function being called.
 * @param overload The overload string identifying the function signature.
 * @param arguments The argument expressions.
 * @param resultTypeIndex The index of the result type in the types array.
 * @return The function call expression.
 */
fun functionCall(
    name: String,
    overload: String,
    arguments: List<TypedExpression>,
    resultTypeIndex: Int
): TypedFunctionCallExpression {
    return TypedFunctionCallExpression(
        evalType = resultTypeIndex,
        name = name,
        overload = overload,
        arguments = arguments
    )
}

/**
 * Creates a member access expression.
 * 
 * @param expression The target expression.
 * @param member The name of the member being accessed.
 * @param isNullChaining Whether this uses null-safe chaining (?.).
 * @param resultTypeIndex The index of the result type in the types array.
 * @return The member access expression.
 */
fun memberAccess(
    expression: TypedExpression,
    member: String,
    isNullChaining: Boolean = false,
    resultTypeIndex: Int
): TypedMemberAccessExpression {
    return TypedMemberAccessExpression(
        evalType = resultTypeIndex,
        expression = expression,
        member = member,
        isNullChaining = isNullChaining
    )
}

/**
 * Creates a member call expression.
 * 
 * @param expression The target expression.
 * @param member The name of the member function being called.
 * @param overload The overload string identifying the method signature.
 * @param arguments The argument expressions.
 * @param isNullChaining Whether this uses null-safe chaining (?.).
 * @param resultTypeIndex The index of the result type in the types array.
 * @return The member call expression.
 */
fun memberCall(
    expression: TypedExpression,
    member: String,
    overload: String,
    arguments: List<TypedExpression>,
    isNullChaining: Boolean = false,
    resultTypeIndex: Int
): TypedMemberCallExpression {
    return TypedMemberCallExpression(
        evalType = resultTypeIndex,
        expression = expression,
        member = member,
        isNullChaining = isNullChaining,
        overload = overload,
        arguments = arguments
    )
}

/**
 * Test helper class for compiling and invoking test functions.
 */
class CompilerTestHelper {
    private val compiler = ScriptCompiler()
    
    companion object {
        private const val TEST_FILE_URI = "test://test.script"
        private const val TEST_FUNCTION_NAME = "testFunction"
    }
    
    /**
     * Compiles a single function and invokes it, returning the result.
     * 
     * @param ast The TypedAST containing the function.
     * @param functionName The function name to invoke.
     * @param args Arguments to pass to the function.
     * @return The result of the function invocation.
     */
    fun compileAndInvoke(
        ast: TypedAst,
        functionName: String = TEST_FUNCTION_NAME,
        vararg args: Any?
    ): Any? {
        val input = CompilationInput(mapOf(TEST_FILE_URI to ast))
        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        return env.invoke(TEST_FILE_URI, functionName, *args)
    }
    
    /**
     * Compiles and invokes a simple test function that returns a value.
     * 
     * @param returnType The return type index.
     * @param returnExpression The expression to return.
     * @param typeBuilder A block to set up types in the builder.
     * @return The result of the function invocation.
     */
    fun compileAndInvokeReturn(
        returnExpression: TypedExpression,
        typeBuilder: TypedAstBuilder.() -> Int
    ): Any? {
        val ast = buildTypedAst {
            val returnType = typeBuilder()
            function(
                name = TEST_FUNCTION_NAME,
                returnType = returnType,
                body = listOf(returnStmt(returnExpression))
            )
        }
        return compileAndInvoke(ast)
    }
}

/**
 * Creates an expression statement.
 * 
 * An expression statement is an expression that is evaluated for its side effects,
 * with the result discarded.
 * 
 * @param expression The expression to evaluate.
 * @return The expression statement.
 */
fun exprStmt(expression: TypedExpression): TypedExpressionStatement {
    return TypedExpressionStatement(expression = expression)
}

/**
 * Creates an expression call expression (direct lambda invocation).
 * 
 * This is used when a lambda stored in a variable is called directly,
 * for example: `f(10)` where `f` is a lambda variable.
 * 
 * @param expression The expression evaluating to a lambda.
 * @param arguments The argument expressions.
 * @param resultTypeIndex The index of the result type in the types array.
 * @return The expression call expression.
 */
fun expressionCall(
    expression: TypedExpression,
    arguments: List<TypedExpression>,
    resultTypeIndex: Int
): TypedExpressionCallExpression {
    return TypedExpressionCallExpression(
        evalType = resultTypeIndex,
        expression = expression,
        arguments = arguments
    )
}
