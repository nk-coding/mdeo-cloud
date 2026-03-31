package com.mdeo.optimizer.moea

import com.mdeo.optimizer.evaluation.WorkerSolutionRef
import org.moeaframework.core.Solution
import org.moeaframework.core.operator.Mutation

/**
 * Variation operator that creates a fresh offspring solution referencing its parent and
 * marking it for mutation on a worker node.
 *
 * Used with delegating algorithms where actual mutation is performed by an external
 * [com.mdeo.optimizer.evaluation.MutationEvaluator]. The offspring is a new, empty
 * MOEA [Solution] that stores the parent's [WorkerSolutionRef] under [PARENT_REF_KEY]
 * and sets [NEEDS_MUTATION_KEY] to `true`. The [EvaluationCoordinator] reads these
 * attributes to build [com.mdeo.optimizer.evaluation.MutationTask]s for offspring that
 * need mutation, as opposed to [com.mdeo.optimizer.evaluation.EvaluationTask]s for
 * solutions that only require fitness evaluation (e.g. during initialization).
 *
 * Because each offspring is a distinct object (not a copy of the parent), there is no
 * ambiguity when the same parent is selected multiple times — each offspring maps 1:1
 * to exactly one mutation task.
 *
 * Implements [Mutation] (which extends `Variation`) so that it can be used with
 * algorithms like PAES that require a [Mutation] operator.
 *
 * Arity is 1 (mutation-only, no crossover).
 */
class DelegatingVariation : Mutation {

    companion object {
        /**
         * Attribute key storing the parent's [WorkerSolutionRef] on an offspring solution.
         */
        const val PARENT_REF_KEY = "parentSolutionRef"

        /**
         * Attribute key indicating that the offspring requires mutation on the worker
         * before fitness evaluation. When absent or `false`, the solution only needs
         * evaluation (no mutation step).
         */
        const val NEEDS_MUTATION_KEY = "needsMutation"
    }

    override fun getName(): String = "DelegatingVariation"

    override fun mutate(parent: Solution): Solution {
        val offspring = Solution(0, parent.numberOfObjectives, parent.numberOfConstraints)
        val parentRef = parent.getWorkerRef()
        if (parentRef != null) {
            offspring.setAttribute(PARENT_REF_KEY, parentRef)
        }
        offspring.setAttribute(NEEDS_MUTATION_KEY, true)
        return offspring
    }
}
