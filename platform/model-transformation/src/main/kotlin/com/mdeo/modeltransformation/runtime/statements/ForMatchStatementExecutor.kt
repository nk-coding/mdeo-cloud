package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.statements.TypedForMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.runtime.StatementExecutor
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.match.MatchExecutor
import com.mdeo.modeltransformation.runtime.match.MatchResult

/**
 * Executor for TypedForMatchStatement.
 *
 * This executor handles iteration over all pattern matches. It finds ALL
 * matches of the pattern in the graph (unlimited) and executes the doBlock
 * once for each match.
 *
 * Unlike while-match which re-evaluates the pattern after each iteration,
 * for-match collects all matches upfront and then iterates over them.
 * This means modifications in the doBlock don't affect which matches
 * are found.
 *
 * The unified executor uses UNLIMITED limit for foreach operations to process
 * all matches in a single Gremlin query with integrated modifications.
 *
 * If no matches are found, the loop body is not executed at all, and
 * Success is returned with the unchanged context. This is NOT a failure.
 *
 * @param matchExecutor The executor used for unified pattern matching and modifications.
 */
class ForMatchStatementExecutor(
    private val matchExecutor: MatchExecutor = MatchExecutor()
) : StatementExecutor {
    
    /**
     * Determines whether this executor can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is a TypedForMatchStatement.
     */
    override fun canExecute(statement: TypedTransformationStatement): Boolean {
        return statement is TypedForMatchStatement
    }
    
    /**
     * Executes a for-match statement.
     *
     * Scope handling per spec:
     * - Enter a new scope (level+1) for ForMatchStatement
     * - The match runs in that scope
     * - The doBlock enters another scope (level+2 from base)
     *
     * Finds all matches of the pattern upfront using executeMatchAll (unlimited),
     * then executes the doBlock for each match. Results are accumulated from all
     * iterations.
     *
     * If no matches are found, returns Success with unchanged context.
     * If any iteration returns Failure or Stopped, the loop terminates.
     *
     * @param statement The for-match statement to execute.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @return The accumulated result from all iterations, or the terminating result.
     */
    override fun execute(
        statement: TypedTransformationStatement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult {
        val forMatchStatement = statement as TypedForMatchStatement
        
        val forContext = context.enterScope()
        
        val allMatches = matchExecutor.executeMatchAll(
            pattern = forMatchStatement.pattern,
            context = forContext,
            engine = engine
        )
        
        if (allMatches.isEmpty()) {
            return TransformationExecutionResult.Success()
        }
        
        return executeIterations(forMatchStatement, allMatches, forContext, context, engine)
    }
    
    /**
     * Executes all iterations of the for-match loop.
     */
    private fun executeIterations(
        statement: TypedForMatchStatement,
        matches: List<MatchResult.Matched>,
        forContext: TransformationExecutionContext,
        baseContext: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult {
        var accumulatedResult = TransformationExecutionResult.Success()
        
        for (matched in matches) {
            accumulatedResult = accumulatedResult.merge(matched)
            val matchedContext = matched.applyToCopy(forContext)
            val iterationResult = engine.executeBlock(statement.doBlock, matchedContext)
            
            when (iterationResult) {
                is TransformationExecutionResult.Success -> {
                    accumulatedResult = accumulatedResult.merge(iterationResult)
                }
                is TransformationExecutionResult.Failure -> return iterationResult
                is TransformationExecutionResult.Stopped -> return iterationResult
            }
        }
        
        return accumulatedResult
    }
    
}

