package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.statements.TypedWhileMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.runtime.StatementExecutor
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.match.MatchExecutor
import com.mdeo.modeltransformation.runtime.match.MatchResult
import com.mdeo.modeltransformation.runtime.match.UnifiedMatchExecutor

/**
 * Executor for TypedWhileMatchStatement.
 *
 * This executor handles repeated execution while a pattern matches.
 * It repeatedly attempts to match the pattern and executes the doBlock
 * for each successful match.
 *
 * The loop terminates when:
 * - The pattern no longer matches (this is NOT a failure)
 * - A statement in the doBlock returns Failure
 * - A statement in the doBlock returns Stopped
 *
 * IMPORTANT: Unlike standalone match statements, when the pattern stops
 * matching in a while-match context, this is NOT a failure. It simply
 * means the loop terminates normally.
 *
 * The unified executor performs matching and modifications in a single Gremlin
 * query with limit=1 (each iteration processes one match).
 *
 * @param matchExecutor The executor used for unified pattern matching and modifications.
 */
class WhileMatchStatementExecutor(
    private val matchExecutor: MatchExecutor = UnifiedMatchExecutor()
) : StatementExecutor {
    
    /**
     * Determines whether this executor can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is a TypedWhileMatchStatement.
     */
    override fun canExecute(statement: TypedTransformationStatement): Boolean {
        return statement is TypedWhileMatchStatement
    }
    
    /**
     * Executes a while-match statement.
     *
     * Repeatedly matches the pattern and executes the doBlock while the pattern
     * matches. Accumulates results from all iterations. When the pattern no longer
     * matches, the loop terminates normally (not a failure).
     *
     * IMPORTANT: Pattern matching uses the base context (without previous iteration's
     * instance bindings) so that each iteration finds a fresh match. Variables from
     * the doBlock are preserved across iterations.
     *
     * @param statement The while-match statement to execute.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @return The accumulated result from all iterations, or the terminating result.
     */
    override fun execute(
        statement: TypedTransformationStatement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult {
        val whileMatchStatement = statement as TypedWhileMatchStatement
        
        var baseContext = context
        var accumulatedResult = TransformationExecutionResult.Success(baseContext)
        
        while (true) {
            val matchResult = matchExecutor.executeMatch(
                pattern = whileMatchStatement.pattern,
                context = baseContext,
                engine = engine
            )
            
            when (matchResult) {
                is MatchResult.NoMatch -> break
                is MatchResult.Matched -> {
                    val iterationResult = executeIteration(
                        whileMatchStatement, matchResult, baseContext, engine
                    )
                    
                    when (iterationResult) {
                        is TransformationExecutionResult.Success -> {
                            baseContext = baseContext.bindVariables(
                                iterationResult.context.getAllVariables()
                            )
                            accumulatedResult = accumulatedResult.merge(iterationResult)
                        }
                        is TransformationExecutionResult.Failure -> return iterationResult
                        is TransformationExecutionResult.Stopped -> return iterationResult
                    }
                }
            }
        }
        
        return accumulatedResult
    }
    
    /**
     * Executes a single iteration of the while-match loop.
     *
     * Executes the doBlock with the updated context. Modifications have already
     * been applied by the unified executor.
     *
     * @param statement The while-match statement.
     * @param matched The match result for this iteration.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @return The result of this iteration.
     */
    private fun executeIteration(
        statement: TypedWhileMatchStatement,
        matched: MatchResult.Matched,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult {
        val updatedContext = matched.applyTo(context)
        val blockResult = engine.executeBlock(statement.doBlock, updatedContext)
        
        return when (blockResult) {
            is TransformationExecutionResult.Success -> {
                TransformationExecutionResult.Success(
                    context = blockResult.context,
                    matchedNodes = matched.matchedNodeIds + blockResult.matchedNodes,
                    matchedEdges = matched.matchedEdgeIds + blockResult.matchedEdges,
                    createdNodes = matched.createdNodeIds + blockResult.createdNodes,
                    deletedNodes = matched.deletedNodeIds + blockResult.deletedNodes,
                    createdEdges = matched.createdEdgeIds + blockResult.createdEdges,
                    deletedEdges = matched.deletedEdgeIds + blockResult.deletedEdges
                )
            }
            is TransformationExecutionResult.Failure -> blockResult
            is TransformationExecutionResult.Stopped -> blockResult
        }
    }
}
