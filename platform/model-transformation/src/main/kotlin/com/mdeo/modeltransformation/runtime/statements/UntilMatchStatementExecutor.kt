package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.statements.TypedUntilMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.runtime.StatementExecutor
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.match.MatchExecutor
import com.mdeo.modeltransformation.runtime.match.MatchResult

/**
 * Executor for TypedUntilMatchStatement.
 *
 * This executor handles repeated execution UNTIL a pattern matches.
 * It executes the doBlock first, then checks if the pattern matches.
 * The loop continues until the pattern DOES match.
 *
 * This is the inverse of while-match: the loop runs while the pattern
 * does NOT match, and stops when it DOES match.
 *
 * The loop terminates when:
 * - The pattern matches (this is the normal termination)
 * - A statement in the doBlock returns Failure
 * - A statement in the doBlock returns Stopped
 *
 * IMPORTANT: This is a do-until loop, meaning the body always executes
 * at least once. When the pattern finally matches, this is NOT a failure -
 * it's the expected termination condition.
 *
 * The unified executor performs matching and modifications in a single Gremlin
 * query with limit=1 (for the final terminating match).
 *
 * @param matchExecutor The executor used for unified pattern matching and modifications.
 */
class UntilMatchStatementExecutor(
    private val matchExecutor: MatchExecutor = MatchExecutor()
) : StatementExecutor {
    
    /**
     * Determines whether this executor can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is a TypedUntilMatchStatement.
     */
    override fun canExecute(statement: TypedTransformationStatement): Boolean {
        return statement is TypedUntilMatchStatement
    }
    
    /**
     * Executes an until-match statement.
     *
     * Repeatedly executes the doBlock and then checks if the pattern matches.
     * The loop continues while the pattern does NOT match.
     * When the pattern matches, the loop terminates normally.
     *
     * @param statement The until-match statement to execute.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @return The accumulated result from all iterations, or the terminating result.
     */
    override fun execute(
        statement: TypedTransformationStatement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult {
        val untilMatchStatement = statement as TypedUntilMatchStatement
        
        var baseContext = TransformationExecutionContext.empty()
            .bindVariables(context.getAllVariables())
        var accumulatedResult = TransformationExecutionResult.Success(context)
        var lastBlockContext: TransformationExecutionContext = context
        
        do {
            val blockResult = engine.executeBlock(untilMatchStatement.doBlock, baseContext)
            
            when (blockResult) {
                is TransformationExecutionResult.Success -> {
                    baseContext = baseContext.bindVariables(blockResult.context.getAllVariables())
                    accumulatedResult = accumulatedResult.merge(blockResult)
                    lastBlockContext = blockResult.context
                }
                is TransformationExecutionResult.Failure -> return blockResult
                is TransformationExecutionResult.Stopped -> return blockResult
            }
            
            val matchResult = matchExecutor.executeMatch(
                pattern = untilMatchStatement.pattern,
                context = baseContext,
                engine = engine
            )
            
            when (matchResult) {
                is MatchResult.Matched -> {
                    return applyMatchAndTerminate(matchResult, lastBlockContext, accumulatedResult)
                }
                is MatchResult.NoMatch -> { /* Continue looping */ }
            }
        } while (true)
    }
    
    /**
     * Applies the final match result and returns the accumulated result.
     *
     * When the pattern finally matches (termination condition), returns the
     * final accumulated result with the match bindings applied. Modifications
     * have already been applied by the unified executor.
     *
     * @param matched The final match result.
     * @param context The current execution context.
     * @param accumulatedResult The accumulated result from all iterations.
     * @return The final result with all accumulated changes plus the final match.
     */
    private fun applyMatchAndTerminate(
        matched: MatchResult.Matched,
        context: TransformationExecutionContext,
        accumulatedResult: TransformationExecutionResult.Success
    ): TransformationExecutionResult.Success {
        val updatedContext = matched.applyTo(context)
        
        return TransformationExecutionResult.Success(
            context = updatedContext,
            matchedNodes = accumulatedResult.matchedNodes + matched.matchedNodeIds,
            matchedEdges = accumulatedResult.matchedEdges + matched.matchedEdgeIds,
            createdNodes = accumulatedResult.createdNodes + matched.createdNodeIds,
            deletedNodes = accumulatedResult.deletedNodes + matched.deletedNodeIds,
            createdEdges = accumulatedResult.createdEdges + matched.createdEdgeIds,
            deletedEdges = accumulatedResult.deletedEdges + matched.deletedEdgeIds
        )
    }
}
