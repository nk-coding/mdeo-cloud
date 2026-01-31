package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.ast.statements.TypedIfExpressionStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationMode
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.runtime.StatementExecutor
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult

/**
 * Executor for TypedIfExpressionStatement.
 *
 * This executor handles conditional execution based on boolean expressions.
 * It evaluates the condition expression and executes the appropriate branch:
 * - If the condition is truthy, executes the thenBlock
 * - If falsy, checks each elseIfBranch in order
 * - If no conditions are true, executes the elseBlock (if present)
 *
 * The result is the combined result from the executed branch. If no branch
 * is executed (condition false with no else), returns Success with unchanged context.
 */
class IfExpressionStatementExecutor : StatementExecutor {
    
    /**
     * Determines whether this executor can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is a TypedIfExpressionStatement.
     */
    override fun canExecute(statement: TypedTransformationStatement): Boolean {
        return statement is TypedIfExpressionStatement
    }
    
    /**
     * Executes an if-expression statement.
     *
     * Evaluates the condition and executes the appropriate branch. If the condition
     * is truthy, the thenBlock is executed. Otherwise, elseIfBranches are checked
     * in order. If no conditions are true and an elseBlock exists, it is executed.
     *
     * @param statement The if-expression statement to execute.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @return The result of executing the selected branch, or Success if no branch executed.
     */
    override fun execute(
        statement: TypedTransformationStatement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult {
        val ifStatement = statement as TypedIfExpressionStatement
        
        // Evaluate main condition
        val conditionValue = evaluateExpression(ifStatement.condition, context, engine)
        
        if (isTruthy(conditionValue)) {
            return engine.executeBlock(ifStatement.thenBlock, context)
        }
        
        // Check else-if branches
        for (branch in ifStatement.elseIfBranches) {
            val branchConditionValue = evaluateExpression(branch.condition, context, engine)
            if (isTruthy(branchConditionValue)) {
                return engine.executeBlock(branch.block, context)
            }
        }
        
        // Execute else block if present
        if (ifStatement.elseBlock != null) {
            return engine.executeBlock(ifStatement.elseBlock, context)
        }
        
        // No branch executed - return success with unchanged context
        return TransformationExecutionResult.Success(context)
    }
    
    /**
     * Evaluates an expression to a value.
     *
     * Attempts to compile and evaluate the expression using the traversal compiler
     * registry first. Falls back to the legacy expression compiler registry if
     * the traversal compilation fails or is not available.
     *
     * @param expression The expression to evaluate.
     * @param context The execution context for variable resolution.
     * @param engine The transformation engine providing the compiler registries.
     * @return The evaluated value, or null if evaluation fails.
     */
    private fun evaluateExpression(
        expression: TypedExpression,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): Any? {
        return evaluateWithTraversalCompiler(expression, engine)
            ?: evaluateWithLegacyCompiler(expression, engine)
    }

    /**
     * Attempts to evaluate an expression using the traversal compiler.
     *
     * @param expression The expression to evaluate.
     * @param engine The transformation engine.
     * @return The evaluated value if successful, or null if compilation fails.
     */
    private fun evaluateWithTraversalCompiler(
        expression: TypedExpression,
        engine: TransformationEngine
    ): Any? {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            return null
        }
        return try {
            val context = TraversalCompilationContext(
                types = emptyList(),
                traversalSource = engine.traversalSource
            )
            val result = engine.expressionCompilerRegistry.compile(expression, context)
            if (result.isConstant) result.constantValue else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Attempts to evaluate an expression using the expression compiler.
     *
     * @param expression The expression to evaluate.
     * @param engine The transformation engine.
     * @return The evaluated value if successful, or null if compilation fails.
     */
    private fun evaluateWithLegacyCompiler(
        expression: TypedExpression,
        engine: TransformationEngine
    ): Any? {
        return try {
            val compilationContext = TraversalCompilationContext(
                types = emptyList(),
                traversalSource = engine.traversalSource
            )
            val result = engine.expressionCompilerRegistry.compile(expression, compilationContext)
            if (result.isConstant) {
                result.constantValue
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Determines if a value is truthy.
     *
     * A value is considered truthy if:
     * - It is a Boolean with value true
     * - It is a non-null, non-zero number
     * - It is a non-empty string
     * - It is a non-empty collection
     * - It is any other non-null object
     *
     * @param value The value to check.
     * @return True if the value is truthy, false otherwise.
     */
    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> value.toDouble() != 0.0
            is String -> value.isNotEmpty()
            is Collection<*> -> value.isNotEmpty()
            else -> true
        }
    }
}
