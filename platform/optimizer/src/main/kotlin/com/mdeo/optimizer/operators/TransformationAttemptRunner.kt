package com.mdeo.optimizer.operators

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.optimizer.graph.GraphBackend
import com.mdeo.optimizer.solution.Solution
import org.slf4j.LoggerFactory

/**
 * Runs a model transformation attempt against a candidate solution.
 *
 * **Critical invariant**: Even failed transformations can modify the graph,
 * so the runner ALWAYS deep-copies the model before executing.
 * The original solution is only updated on success.
 *
 * This design also prepares for future distributed execution: each attempt
 * is a self-contained unit of work (candidate + operator + seed) that can
 * be shipped to a remote node.
 *
 * @param transformations Map of transformation path to its compiled TypedAst.
 * @param metamodelData The metamodel used by the transformations.
 */
class TransformationAttemptRunner(
    private val transformations: Map<String, TypedAst>,
    private val metamodelData: MetamodelData
) {
    private val logger = LoggerFactory.getLogger(TransformationAttemptRunner::class.java)

    /**
     * Attempts to apply the named transformation to the given solution.
     *
     * @param solution The candidate solution (will NOT be modified on failure).
     * @param transformationPath Path identifying which transformation to apply.
     * @return true if the transformation was applied successfully, false otherwise.
     */
    fun tryApply(solution: Solution, transformationPath: String): Boolean {
        val typedAst = transformations[transformationPath]
            ?: throw IllegalArgumentException("Unknown transformation: $transformationPath")

        // ALWAYS copy before transform — even failed transforms can mutate the graph
        val snapshot = solution.graphBackend.deepCopy()

        return try {
            val g = snapshot.traversal()
            val engine = TransformationEngine.create(g, typedAst, metamodelData, deterministic = false)

            // We need to load existing graph data into the engine's scope.
            // The snapshot already contains the model; the engine operates on the same traversal.
            val result = engine.execute()

            when (result) {
                is TransformationExecutionResult.Success -> {
                    // Transformation succeeded on the snapshot — swap it into the solution
                    replaceGraphBackend(solution, snapshot)
                    true
                }
                is TransformationExecutionResult.Stopped -> {
                    if (result.isNormalStop) {
                        replaceGraphBackend(solution, snapshot)
                        true
                    } else {
                        // Killed — discard snapshot
                        snapshot.close()
                        false
                    }
                }
                is TransformationExecutionResult.Failure -> {
                    logger.debug("Transformation $transformationPath failed: ${result.reason}")
                    snapshot.close()
                    false
                }
            }
        } catch (e: Exception) {
            logger.warn("Transformation $transformationPath threw exception: ${e.message}")
            snapshot.close()
            false
        }
    }

    /**
     * Replaces the graph backend in a solution with the successfully-transformed snapshot.
     * Closes the old backend.
     */
    private fun replaceGraphBackend(solution: Solution, newBackend: GraphBackend) {
        solution.graphBackend.close()
        solution.graphBackend = newBackend
    }
}
