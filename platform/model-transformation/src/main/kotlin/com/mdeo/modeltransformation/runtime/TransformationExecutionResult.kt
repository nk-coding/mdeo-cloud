package com.mdeo.modeltransformation.runtime

/**
 * Represents the result of executing a model transformation.
 *
 * This sealed interface provides a type-safe way to handle transformation
 * execution outcomes. A transformation can either complete successfully
 * or fail with a reason.
 */
sealed interface TransformationExecutionResult {
    
    /**
     * Indicates that the transformation completed successfully.
     *
     * @param context The final execution context after transformation.
     * @param matchedNodes Set of vertex IDs that were matched during execution.
     * @param createdNodes Set of vertex IDs that were created during execution.
     * @param deletedNodes Set of vertex IDs that were deleted during execution.
     * @param matchedEdges Set of edge IDs that were matched during execution.
     * @param createdEdges Set of edge IDs that were created during execution.
     * @param deletedEdges Set of edge IDs that were deleted during execution.
     */
    data class Success(
        val context: TransformationExecutionContext,
        val matchedNodes: Set<Any> = emptySet(),
        val createdNodes: Set<Any> = emptySet(),
        val deletedNodes: Set<Any> = emptySet(),
        val matchedEdges: Set<Any> = emptySet(),
        val createdEdges: Set<Any> = emptySet(),
        val deletedEdges: Set<Any> = emptySet()
    ) : TransformationExecutionResult {
        
        /**
         * Creates a new Success result with additional matched nodes.
         *
         * @param nodeIds The node IDs that were matched.
         * @return A new Success with the additional matched nodes.
         */
        fun withMatchedNodes(vararg nodeIds: Any): Success {
            return copy(matchedNodes = matchedNodes + nodeIds.toSet())
        }
        
        /**
         * Creates a new Success result with additional created nodes.
         *
         * @param nodeIds The node IDs that were created.
         * @return A new Success with the additional created nodes.
         */
        fun withCreatedNodes(vararg nodeIds: Any): Success {
            return copy(createdNodes = createdNodes + nodeIds.toSet())
        }
        
        /**
         * Creates a new Success result with additional deleted nodes.
         *
         * @param nodeIds The node IDs that were deleted.
         * @return A new Success with the additional deleted nodes.
         */
        fun withDeletedNodes(vararg nodeIds: Any): Success {
            return copy(deletedNodes = deletedNodes + nodeIds.toSet())
        }
        
        /**
         * Creates a new Success result with additional matched edges.
         *
         * @param edgeIds The edge IDs that were matched.
         * @return A new Success with the additional matched edges.
         */
        fun withMatchedEdges(vararg edgeIds: Any): Success {
            return copy(matchedEdges = matchedEdges + edgeIds.toSet())
        }
        
        /**
         * Creates a new Success result with additional created edges.
         *
         * @param edgeIds The edge IDs that were created.
         * @return A new Success with the additional created edges.
         */
        fun withCreatedEdges(vararg edgeIds: Any): Success {
            return copy(createdEdges = createdEdges + edgeIds.toSet())
        }
        
        /**
         * Creates a new Success result with additional deleted edges.
         *
         * @param edgeIds The edge IDs that were deleted.
         * @return A new Success with the additional deleted edges.
         */
        fun withDeletedEdges(vararg edgeIds: Any): Success {
            return copy(deletedEdges = deletedEdges + edgeIds.toSet())
        }
        
        /**
         * Creates a new Success result with an updated context.
         *
         * @param newContext The new execution context.
         * @return A new Success with the updated context.
         */
        fun withContext(newContext: TransformationExecutionContext): Success {
            return copy(context = newContext)
        }
        
        /**
         * Merges another Success result into this one.
         *
         * The contexts are not merged; the other result's context takes precedence.
         * All node and edge sets are combined.
         *
         * @param other The other Success result to merge.
         * @return A new Success with merged results.
         */
        fun merge(other: Success): Success {
            return Success(
                context = other.context,
                matchedNodes = matchedNodes + other.matchedNodes,
                createdNodes = createdNodes + other.createdNodes,
                deletedNodes = deletedNodes + other.deletedNodes,
                matchedEdges = matchedEdges + other.matchedEdges,
                createdEdges = createdEdges + other.createdEdges,
                deletedEdges = deletedEdges + other.deletedEdges
            )
        }
    }
    
    /**
     * Indicates that the transformation failed.
     *
     * @param reason A human-readable description of why the transformation failed.
     * @param context The execution context at the time of failure.
     * @param failedAt Optional identifier of the statement or pattern that caused the failure.
     */
    data class Failure(
        val reason: String,
        val context: TransformationExecutionContext? = null,
        val failedAt: String? = null
    ) : TransformationExecutionResult {
        
        /**
         * Creates a new Failure with additional context about where the failure occurred.
         *
         * @param location Description of where the failure occurred.
         * @return A new Failure with the location information.
         */
        fun at(location: String): Failure {
            return copy(failedAt = location)
        }
        
        /**
         * Creates a new Failure with the execution context.
         *
         * @param ctx The execution context at the time of failure.
         * @return A new Failure with the context attached.
         */
        fun withContext(ctx: TransformationExecutionContext): Failure {
            return copy(context = ctx)
        }
    }
    
    /**
     * Indicates that the transformation was explicitly stopped.
     *
     * This is different from success or failure - it represents an intentional
     * termination via a stop/kill statement.
     *
     * @param keyword The keyword used to stop: "stop" for normal termination,
     *                "kill" for immediate termination.
     * @param context The execution context at the time of stopping.
     */
    data class Stopped(
        val keyword: String,
        val context: TransformationExecutionContext
    ) : TransformationExecutionResult {
        
        /**
         * Returns true if this was a normal stop (not a kill).
         */
        val isNormalStop: Boolean get() = keyword == "stop"
        
        /**
         * Returns true if this was a kill (immediate termination).
         */
        val isKill: Boolean get() = keyword == "kill"
    }
}

/**
 * Extension function to check if a result is successful.
 *
 * @return True if the result is a [TransformationExecutionResult.Success].
 */
fun TransformationExecutionResult.isSuccess(): Boolean = this is TransformationExecutionResult.Success

/**
 * Extension function to check if a result is a failure.
 *
 * @return True if the result is a [TransformationExecutionResult.Failure].
 */
fun TransformationExecutionResult.isFailure(): Boolean = this is TransformationExecutionResult.Failure

/**
 * Extension function to check if execution was stopped.
 *
 * @return True if the result is a [TransformationExecutionResult.Stopped].
 */
fun TransformationExecutionResult.isStopped(): Boolean = this is TransformationExecutionResult.Stopped

/**
 * Extension function to get the Success result or null.
 *
 * @return The [TransformationExecutionResult.Success] if successful, null otherwise.
 */
fun TransformationExecutionResult.successOrNull(): TransformationExecutionResult.Success? =
    this as? TransformationExecutionResult.Success

/**
 * Extension function to get the Failure result or null.
 *
 * @return The [TransformationExecutionResult.Failure] if failed, null otherwise.
 */
fun TransformationExecutionResult.failureOrNull(): TransformationExecutionResult.Failure? =
    this as? TransformationExecutionResult.Failure
