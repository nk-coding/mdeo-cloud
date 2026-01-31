package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext

/**
 * Result of executing a pattern match operation.
 *
 * This sealed interface represents the possible outcomes of attempting to match
 * a pattern against the graph. A match can either succeed (finding one or more
 * matches) or fail to find any matching subgraph.
 */
sealed interface MatchResult {
    
    /**
     * Indicates that the pattern matched successfully.
     *
     * Contains all the bindings established by the match, including both
     * variable bindings and instance mappings. Also includes information
     * about which nodes were matched vs created.
     *
     * @param bindings Variable bindings established by the match.
     * @param instanceMappings Maps instance names to their matched/created vertex IDs.
     * @param matchedNodeIds Set of vertex IDs that were matched (not created).
     * @param matchedEdgeIds Set of edge IDs that were matched (not created).
     * @param createdNodeIds Set of vertex IDs that were created by the pattern.
     * @param createdEdgeIds Set of edge IDs that were created by the pattern.
     * @param deletedNodeIds Set of vertex IDs that were deleted by the pattern.
     * @param deletedEdgeIds Set of edge IDs that were deleted by the pattern.
     */
    data class Matched(
        val bindings: Map<String, Any?> = emptyMap(),
        val instanceMappings: Map<String, Any> = emptyMap(),
        val matchedNodeIds: Set<Any> = emptySet(),
        val matchedEdgeIds: Set<Any> = emptySet(),
        val createdNodeIds: Set<Any> = emptySet(),
        val createdEdgeIds: Set<Any> = emptySet(),
        val deletedNodeIds: Set<Any> = emptySet(),
        val deletedEdgeIds: Set<Any> = emptySet()
    ) : MatchResult {
        
        /**
         * Applies this match result to an execution context.
         *
         * @param context The context to update with the match bindings.
         * @return A new context with the bindings applied.
         */
        fun applyTo(context: TransformationExecutionContext): TransformationExecutionContext {
            return context
                .bindVariables(bindings)
                .bindInstances(instanceMappings)
        }
    }
    
    /**
     * Indicates that no match was found.
     *
     * @param reason Optional explanation of why the match failed.
     */
    data class NoMatch(
        val reason: String? = null
    ) : MatchResult
}

/**
 * Extension function to check if a match was successful.
 *
 * @return True if the result is a [MatchResult.Matched].
 */
fun MatchResult.isMatched(): Boolean = this is MatchResult.Matched

/**
 * Extension function to check if a match failed.
 *
 * @return True if the result is a [MatchResult.NoMatch].
 */
fun MatchResult.isNoMatch(): Boolean = this is MatchResult.NoMatch

/**
 * Extension function to get the Matched result or null.
 *
 * @return The [MatchResult.Matched] if successful, null otherwise.
 */
fun MatchResult.matchedOrNull(): MatchResult.Matched? = this as? MatchResult.Matched
