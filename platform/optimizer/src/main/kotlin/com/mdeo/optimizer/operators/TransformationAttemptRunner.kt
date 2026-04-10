package com.mdeo.optimizer.operators

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.optimizer.solution.Solution
import org.slf4j.LoggerFactory

/**
 * Runs a model transformation attempt against a candidate solution.
 *
 * Nondeterministic behaviour is now reset automatically inside
 * [com.mdeo.modeltransformation.runtime.match.MatchExecutor] before every individual
 * match step, so each match within a single transformation execution sees a freshly
 * shuffled vertex iteration order. This prevents earlier matches from constraining
 * the possible outcomes of later ones.
 *
 * @param transformations Map of transformation path to its compiled TypedAst.
 */
class TransformationAttemptRunner(
    private val transformations: Map<String, TypedAst>
) {
    private val logger = LoggerFactory.getLogger(TransformationAttemptRunner::class.java)

    /**
     * Attempts to apply the named transformation to the given solution.
     *
     * Creates a [TransformationEngine] in non-deterministic mode and runs the
     * compiled transformation AST against the solution's model graph. On success
     * the graph is modified in place; on failure the graph is left unchanged.
     *
     * @param solution The candidate solution (modified in place on success).
     * @param transformationPath Path identifying which transformation to apply.
     * @return true if the transformation was applied successfully, false otherwise.
     */
    fun tryApply(solution: Solution, transformationPath: String): Boolean {
        val typedAst = transformations[transformationPath]
            ?: throw IllegalArgumentException("Unknown transformation: $transformationPath")

        return try {
            val engine = TransformationEngine.create(
                solution.modelGraph, typedAst, deterministic = false
            )
            val result = engine.execute()

            when (result) {
                is TransformationExecutionResult.Success -> true
                is TransformationExecutionResult.Stopped -> result.isNormalStop
                is TransformationExecutionResult.Failure -> false
            }
        } catch (e: Exception) {
            logger.warn("Transformation $transformationPath threw exception: ${e.message}")
            false
        }
    }
}
