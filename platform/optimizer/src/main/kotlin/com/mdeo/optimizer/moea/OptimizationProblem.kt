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

class OptimizationProblem(
    private val objectives: List<GuidanceFunction>,
    private val constraints: List<GuidanceFunction>,
    private val solutionGenerator: SolutionGenerator,
    private val scriptTimeoutMs: Long? = null
) : AbstractProblem(1, objectives.size, constraints.size) {

    private val logger = LoggerFactory.getLogger(OptimizationProblem::class.java)

    private val executor = if (scriptTimeoutMs != null) {
        val counter = AtomicInteger()
        Executors.newCachedThreadPool(ThreadFactory { runnable ->
            Thread(runnable, "script-eval-${counter.incrementAndGet()}").also { it.isDaemon = true }
        })
    } else null

    override fun evaluate(solution: MoeaSolution) {
        val optSolution = (solution as OptimizationSolution)
        val candidate = optSolution.getOptimizationSolution()

        val evaluation: () -> Unit = {
            objectives.forEachIndexed { index, fn ->
                optSolution.setObjectiveValue(index, fn.computeFitness(candidate))
            }
            constraints.forEachIndexed { index, fn ->
                optSolution.setConstraintValue(index, fn.computeFitness(candidate))
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
