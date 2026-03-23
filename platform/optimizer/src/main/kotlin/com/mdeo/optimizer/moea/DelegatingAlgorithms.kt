package com.mdeo.optimizer.moea

import org.moeaframework.algorithm.IBEA
import org.moeaframework.algorithm.NSGAII
import org.moeaframework.algorithm.PAES
import org.moeaframework.algorithm.PESA2
import org.moeaframework.algorithm.RandomSearch
import org.moeaframework.algorithm.SMSEMOA
import org.moeaframework.algorithm.SPEA2
import org.moeaframework.algorithm.VEGA
import org.moeaframework.core.Solution
import org.moeaframework.core.fitness.FitnessEvaluator
import org.moeaframework.core.fitness.HypervolumeFitnessEvaluator
import org.moeaframework.core.initialization.Initialization
import org.moeaframework.core.operator.Mutation
import org.moeaframework.core.operator.Variation
import org.moeaframework.core.population.NondominatedPopulation
import org.moeaframework.core.population.NondominatedSortingPopulation
import org.moeaframework.core.population.Population
import org.moeaframework.problem.Problem

/**
 * NSGA-II subclass that delegates evaluation to an [EvaluationCoordinator].
 *
 * Overrides `evaluateAll` and `evaluate` to route solutions through the coordinator,
 * and overrides `iterate` to detect discarded solutions before each generation.
 * Discards are queued and sent alongside the next evaluation batch.
 *
 * @param problem The problem definition (typically [DelegatingProblem]).
 * @param populationSize The population size.
 * @param variation The variation operator (typically [PassThroughVariation]).
 * @param initialization The initialization strategy.
 * @param coordinator The evaluation coordinator managing solution lifecycle.
 */
class DelegatingNSGAII(
    problem: Problem,
    populationSize: Int,
    variation: Variation,
    initialization: Initialization,
    private val coordinator: EvaluationCoordinator
) : NSGAII(
    problem,
    populationSize,
    NondominatedSortingPopulation(),
    null,
    null,
    variation,
    initialization
) {
    override fun evaluateAll(solutions: Iterable<Solution>) {
        numberOfEvaluations += coordinator.batchEvaluateAndUpdate(solutions)
    }

    override fun evaluate(solution: Solution) {
        coordinator.singleEvaluateAndUpdate(solution)
        numberOfEvaluations++
    }

    override fun iterate() {
        coordinator.prepareIteration(getPopulation())
        super.iterate()
    }
}

/**
 * SPEA2 subclass that delegates evaluation to an [EvaluationCoordinator].
 *
 * @param problem The problem definition (typically [DelegatingProblem]).
 * @param populationSize The population size.
 * @param initialization The initialization strategy.
 * @param variation The variation operator (typically [PassThroughVariation]).
 * @param numberOfOffspring The number of offspring generated each iteration.
 * @param k The niching parameter for crowding distance (typically `1`).
 * @param coordinator The evaluation coordinator managing solution lifecycle.
 */
class DelegatingSPEA2(
    problem: Problem,
    populationSize: Int,
    initialization: Initialization,
    variation: Variation,
    numberOfOffspring: Int,
    k: Int,
    private val coordinator: EvaluationCoordinator
) : SPEA2(problem, populationSize, initialization, variation, numberOfOffspring, k) {

    override fun evaluateAll(solutions: Iterable<Solution>) {
        numberOfEvaluations += coordinator.batchEvaluateAndUpdate(solutions)
    }

    override fun evaluate(solution: Solution) {
        coordinator.singleEvaluateAndUpdate(solution)
        numberOfEvaluations++
    }

    override fun iterate() {
        coordinator.prepareIteration(getPopulation())
        super.iterate()
    }
}

/**
 * IBEA subclass that delegates evaluation to an [EvaluationCoordinator].
 *
 * @param problem The problem definition (typically [DelegatingProblem]).
 * @param populationSize The population size.
 * @param initialization The initialization strategy.
 * @param variation The variation operator (typically [PassThroughVariation]).
 * @param fitnessEvaluator The indicator-based fitness evaluator.
 * @param coordinator The evaluation coordinator managing solution lifecycle.
 */
class DelegatingIBEA(
    problem: Problem,
    populationSize: Int,
    initialization: Initialization,
    variation: Variation,
    fitnessEvaluator: HypervolumeFitnessEvaluator,
    private val coordinator: EvaluationCoordinator
) : IBEA(problem, populationSize, null, initialization, variation, fitnessEvaluator) {

    override fun evaluateAll(solutions: Iterable<Solution>) {
        numberOfEvaluations += coordinator.batchEvaluateAndUpdate(solutions)
    }

    override fun evaluate(solution: Solution) {
        coordinator.singleEvaluateAndUpdate(solution)
        numberOfEvaluations++
    }

    override fun iterate() {
        coordinator.prepareIteration(getPopulation())
        super.iterate()
    }
}

/**
 * SMS-EMOA subclass that delegates evaluation to an [EvaluationCoordinator].
 *
 * SMS-EMOA evaluates a single offspring per iteration via `evaluate()`, making
 * the single-solution override critical.
 *
 * @param problem The problem definition (typically [DelegatingProblem]).
 * @param populationSize The population size.
 * @param initialization The initialization strategy.
 * @param variation The variation operator (typically [PassThroughVariation]).
 * @param fitnessEvaluator The fitness evaluator for SMS-EMOA selection.
 * @param coordinator The evaluation coordinator managing solution lifecycle.
 */
class DelegatingSMSEMOA(
    problem: Problem,
    populationSize: Int,
    initialization: Initialization,
    variation: Variation,
    fitnessEvaluator: FitnessEvaluator,
    private val coordinator: EvaluationCoordinator
) : SMSEMOA(problem, populationSize, initialization, variation, fitnessEvaluator) {

    override fun evaluateAll(solutions: Iterable<Solution>) {
        numberOfEvaluations += coordinator.batchEvaluateAndUpdate(solutions)
    }

    override fun evaluate(solution: Solution) {
        coordinator.singleEvaluateAndUpdate(solution)
        numberOfEvaluations++
    }

    override fun iterate() {
        coordinator.prepareIteration(getPopulation())
        super.iterate()
    }
}

/**
 * VEGA subclass that delegates evaluation to an [EvaluationCoordinator].
 *
 * @param problem The problem definition (typically [DelegatingProblem]).
 * @param populationSize The population size.
 * @param initialization The initialization strategy.
 * @param variation The variation operator (typically [PassThroughVariation]).
 * @param coordinator The evaluation coordinator managing solution lifecycle.
 */
class DelegatingVEGA(
    problem: Problem,
    populationSize: Int,
    initialization: Initialization,
    variation: Variation,
    private val coordinator: EvaluationCoordinator
) : VEGA(problem, populationSize, Population(), NondominatedPopulation(), initialization, variation) {

    override fun evaluateAll(solutions: Iterable<Solution>) {
        numberOfEvaluations += coordinator.batchEvaluateAndUpdate(solutions)
    }

    override fun evaluate(solution: Solution) {
        coordinator.singleEvaluateAndUpdate(solution)
        numberOfEvaluations++
    }

    override fun iterate() {
        coordinator.prepareIteration(getPopulation())
        super.iterate()
    }
}

/**
 * PESA2 subclass that delegates evaluation to an [EvaluationCoordinator].
 *
 * PESA2 uses an adaptive grid archive as the primary solution store, so lifecycle
 * tracking operates on the archive rather than the population.
 *
 * @param problem The problem definition (typically [DelegatingProblem]).
 * @param populationSize The population size.
 * @param variation The variation operator (typically [PassThroughVariation]).
 * @param initialization The initialization strategy.
 * @param bisections The number of bisections for the adaptive grid archive.
 * @param archiveSize The capacity of the adaptive grid archive.
 * @param coordinator The evaluation coordinator managing solution lifecycle.
 */
class DelegatingPESA2(
    problem: Problem,
    populationSize: Int,
    variation: Variation,
    initialization: Initialization,
    bisections: Int,
    archiveSize: Int,
    private val coordinator: EvaluationCoordinator
) : PESA2(problem, populationSize, variation, initialization, bisections, archiveSize) {

    override fun evaluateAll(solutions: Iterable<Solution>) {
        numberOfEvaluations += coordinator.batchEvaluateAndUpdate(solutions)
    }

    override fun evaluate(solution: Solution) {
        coordinator.singleEvaluateAndUpdate(solution)
        numberOfEvaluations++
    }

    override fun iterate() {
        coordinator.prepareIteration(archive)
        super.iterate()
    }
}

/**
 * PAES subclass that delegates evaluation to an [EvaluationCoordinator].
 *
 * PAES is a (1+1) evolution strategy that evaluates a single offspring per iteration
 * via `evaluate()`. It maintains both a population (size 1) and an archive, so
 * lifecycle tracking covers the combined set.
 *
 * @param problem The problem definition (typically [DelegatingProblem]).
 * @param mutation The mutation operator (typically [PassThroughVariation]).
 * @param bisections The number of bisections for the adaptive grid archive.
 * @param archiveSize The capacity of the adaptive grid archive.
 * @param coordinator The evaluation coordinator managing solution lifecycle.
 */
class DelegatingPAES(
    problem: Problem,
    mutation: Mutation,
    bisections: Int,
    archiveSize: Int,
    private val coordinator: EvaluationCoordinator
) : PAES(problem, mutation, bisections, archiveSize) {

    override fun evaluateAll(solutions: Iterable<Solution>) {
        numberOfEvaluations += coordinator.batchEvaluateAndUpdate(solutions)
    }

    override fun evaluate(solution: Solution) {
        coordinator.singleEvaluateAndUpdate(solution)
        numberOfEvaluations++
    }

    override fun iterate() {
        val allSolutions = Population()
        allSolutions.addAll(getPopulation())
        allSolutions.addAll(archive)
        coordinator.prepareIteration(allSolutions)
        super.iterate()
    }
}

/**
 * RandomSearch subclass that delegates evaluation to an [EvaluationCoordinator].
 *
 * RandomSearch generates random solutions each iteration and adds non-dominated ones
 * to an archive. Lifecycle tracking operates on the result set.
 *
 * @param problem The problem definition (typically [DelegatingProblem]).
 * @param sampleSize The number of solutions sampled each iteration.
 * @param initialization The initialization strategy.
 * @param archive The non-dominated archive.
 * @param coordinator The evaluation coordinator managing solution lifecycle.
 */
class DelegatingRandomSearch(
    problem: Problem,
    sampleSize: Int,
    initialization: Initialization,
    archive: NondominatedPopulation,
    private val coordinator: EvaluationCoordinator
) : RandomSearch(problem, sampleSize, initialization, archive) {

    override fun evaluateAll(solutions: Iterable<Solution>) {
        numberOfEvaluations += coordinator.batchEvaluateAndUpdate(solutions)
    }

    override fun evaluate(solution: Solution) {
        coordinator.singleEvaluateAndUpdate(solution)
        numberOfEvaluations++
    }

    override fun iterate() {
        coordinator.prepareIteration(result)
        super.iterate()
    }
}
