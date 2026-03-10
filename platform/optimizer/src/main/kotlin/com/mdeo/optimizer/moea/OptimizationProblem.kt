package com.mdeo.optimizer.moea

import com.mdeo.optimizer.guidance.GuidanceFunction
import com.mdeo.optimizer.solution.Solution
import org.moeaframework.core.Solution as MoeaSolution
import org.moeaframework.problem.AbstractProblem
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * MOEA Framework problem definition for the optimizer.
 *
 * Evaluates candidate solutions using configured objective and constraint functions.
 * Ported from MoeaOptimisationProblem.java.
 *
 * @param objectives The objective fitness functions.
 * @param constraints The constraint fitness functions.
 * @param solutionGenerator Generates new/initial solutions.
 * @param scriptTimeoutMs Combined per-solution evaluation timeout in milliseconds;
 *   computed externally as `perScriptTimeout * (objectives + constraints)`.
 *   When `null`, no timeout is enforced.
 */
class OptimizationProblem(
    private val objectives: List<GuidanceFunction>,
    private val constraints: List<GuidanceFunction>,
    private val solutionGenerator: SolutionGenerator,
    private val scriptTimeoutMs: Long? = null
) : AbstractProblem(1, objectives.size, constraints.size) {

    private val logger = LoggerFactory.getLogger(OptimizationProblem::class.java)

    /**
     * Thread pool for timed script evaluations.  Uses daemon threads so stuck evaluations do not
     * prevent JVM shutdown.  Only created when [scriptTimeoutMs] is non-null.
     */
    private val executor = if (scriptTimeoutMs != null) {
        val counter = AtomicInteger()
        Executors.newCachedThreadPool(ThreadFactory { runnable ->
            Thread(runnable, "script-eval-${counter.incrementAndGet()}").also { it.isDaemon = true }
        })
    } else null

    /**
     * Evaluates one candidate solution by invoking all objective and constraint functions.
     *
     * When [scriptTimeoutMs] is set the entire evaluation block runs in a separate daemon thread
     * and must complete within the combined timeout.  A timeout causes the whole optimization run
     * to fail by throwing [RuntimeException].
     *
     * @param solution The MOEA Framework solution whose objective and constraint values are set.
     */
    override fun evaluate(solution: MoeaSolution) {
        val optSolution = (solution as OptimizationSolution)
        val candidate = optSolution.getOptimizationSolution()

        val evaluation: () -> Unit = {
            objectives.forEachIndexed { index, objective ->
                optSolution.setObjectiveValue(index, objective.computeFitness(candidate))
            }
            constraints.forEachIndexed { index, constraint ->
                optSolution.setConstraintValue(index, constraint.computeFitness(candidate))
            }
        }

        if (executor != null && scriptTimeoutMs != null) {
            val future = executor.submit(evaluation)
            try {
                future.get(scriptTimeoutMs, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                future.cancel(true)
                val msg = "Script evaluation timed out after ${scriptTimeoutMs}ms " +
                    "(${objectives.size} objective(s), ${constraints.size} constraint(s))"
                logger.error(msg)
                throw RuntimeException(msg)
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        } else {
            evaluation()
        }
    }

    override fun newSolution(): MoeaSolution {
        return solutionGenerator.createNewSolution(numberOfObjectives, numberOfConstraints)
    }

    override fun close() {
        executor?.shutdown()
        super.close()
    }
}
