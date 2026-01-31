package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.ast.statements.TypedWhileExpressionStatement
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
 * Executor for TypedWhileExpressionStatement.
 *
 * This executor handles repeated execution based on a boolean expression.
 * It evaluates the condition before each iteration and executes the block
 * as long as the condition remains truthy.
 *
 * The executor accumulates results from all iterations. If any iteration
 * results in Failure or Stopped, the loop terminates and that result is returned.
 */
class WhileExpressionStatementExecutor : StatementExecutor {
    
    /**
     * Determines whether this executor can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is a TypedWhileExpressionStatement.
     */
    override fun canExecute(statement: TypedTransformationStatement): Boolean {
        return statement is TypedWhileExpressionStatement
    }
    
    /**
     * Executes a while-expression statement.
     *
     * Repeatedly evaluates the condition and executes the block as long as
     * the condition is truthy. Accumulates results from all iterations.
     * Terminates immediately if any iteration returns Failure or Stopped.
     *
     * @param statement The while-expression statement to execute.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @return The accumulated result from all iterations, or the terminating result.
     */
    override fun execute(
        statement: TypedTransformationStatement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult {
        val whileStatement = statement as TypedWhileExpressionStatement
        
        // Use baseContext for block execution - start fresh with only variables from original context
        // This ensures each iteration can find fresh instances rather than trying to match
        // previously bound (and potentially deleted) instances
        var baseContext = TransformationExecutionContext.empty()
            .bindVariables(context.getAllVariables())
        var accumulatedResult = TransformationExecutionResult.Success(context)
        
        while (true) {
            val conditionValue = evaluateExpression(whileStatement.condition, baseContext, engine)
            
            if (!isTruthy(conditionValue)) {
                break
            }
            
            val iterationResult = engine.executeBlock(whileStatement.block, baseContext)
            
            when (iterationResult) {
                is TransformationExecutionResult.Success -> {
                    // Propagate variable bindings to next iteration but not instance bindings
                    baseContext = baseContext.bindVariables(iterationResult.context.getAllVariables())
                    accumulatedResult = accumulatedResult.merge(iterationResult)
                }
                is TransformationExecutionResult.Failure -> return iterationResult
                is TransformationExecutionResult.Stopped -> return iterationResult
            }
        }
        
        return accumulatedResult
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
