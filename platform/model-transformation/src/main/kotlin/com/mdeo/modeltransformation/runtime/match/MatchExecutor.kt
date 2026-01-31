package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationEngine

/**
 * Interface for executing pattern match operations with integrated modifications.
 *
 * A MatchExecutor is responsible for executing a pattern match against the graph,
 * finding subgraphs that satisfy the pattern's constraints, applying modifications
 * (create, delete, update), and returning the resulting bindings.
 *
 * The executor handles:
 * - Matching object instances against vertices
 * - Matching links against edges
 * - Evaluating property constraints (== comparisons)
 * - Evaluating where clause conditions
 * - Handling forbid modifiers (negative patterns)
 * - Creating new vertices and edges (create modifier)
 * - Deleting matched vertices and edges (delete modifier)
 * - Updating property values (= operator)
 *
 * All operations are performed in a single Gremlin query for atomicity.
 *
 * @see MatchResult
 * @see TypedPattern
 */
interface MatchExecutor {
    
    companion object {
        /** Default limit for single match operations. */
        const val DEFAULT_LIMIT = 1L
        
        /** Value indicating no limit (for foreach operations). */
        const val UNLIMITED = -1L
    }
    
    /**
     * Executes a pattern match with modifications, returning the first match.
     *
     * This method applies limit=1 by default, finding and processing only
     * the first match. Use [executeMatchAll] for foreach use cases.
     *
     * @param pattern The pattern to match.
     * @param context The current execution context, providing variable values and instance mappings.
     * @param engine The transformation engine, for accessing the graph and compiling expressions.
     * @return The match result containing bindings if successful, or NoMatch if no match found.
     */
    fun executeMatch(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): MatchResult
    
    /**
     * Executes a pattern match with modifications, returning all matches.
     *
     * Unlike [executeMatch] which returns only the first match, this method
     * finds and processes all distinct matches of the pattern in the graph.
     * This is used for foreach-style iteration.
     *
     * @param pattern The pattern to match.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @return A list of all matches found, empty if no matches.
     */
    fun executeMatchAll(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): List<MatchResult.Matched>
    
    /**
     * Executes a pattern match with a specified limit.
     *
     * Allows controlling how many matches are processed. Use DEFAULT_LIMIT (1)
     * for single match, or UNLIMITED (-1) for all matches.
     *
     * @param pattern The pattern to match.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @param limit Maximum number of matches to process (-1 for unlimited).
     * @return List of match results with modifications applied.
     */
    fun executeMatchWithLimit(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long
    ): List<MatchResult.Matched> {
        return if (limit == DEFAULT_LIMIT) {
            val result = executeMatch(pattern, context, engine)
            if (result is MatchResult.Matched) listOf(result) else emptyList()
        } else {
            executeMatchAll(pattern, context, engine)
        }
    }
    
    /**
     * Checks if the pattern can potentially match.
     *
     * This is a quick check that returns false if the pattern definitely cannot
     * match (e.g., required types don't exist in the schema). Returns true if
     * the pattern might match, requiring full execution to determine.
     *
     * @param pattern The pattern to check.
     * @param engine The transformation engine.
     * @return True if the pattern might match, false if it definitely cannot.
     */
    fun canMatch(pattern: TypedPattern, engine: TransformationEngine): Boolean {
        return true
    }
}
