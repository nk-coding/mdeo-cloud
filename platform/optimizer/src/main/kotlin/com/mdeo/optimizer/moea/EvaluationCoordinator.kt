package com.mdeo.optimizer.moea

import com.mdeo.optimizer.evaluation.EvaluationResult
import com.mdeo.optimizer.evaluation.MutationEvaluator
import com.mdeo.optimizer.evaluation.MutationTask
import com.mdeo.optimizer.evaluation.NodeBatch
import com.mdeo.optimizer.evaluation.SolutionImportData
import com.mdeo.optimizer.evaluation.WorkerSolutionRef
import kotlinx.coroutines.runBlocking
import org.moeaframework.core.Solution
import org.moeaframework.core.population.Population
import org.slf4j.LoggerFactory

/**
 * Bridges MOEA Framework's synchronous evaluation callbacks into the asynchronous
 * [MutationEvaluator] contract, managing solution references and lifecycle.
 *
 * Detects initialization phase (no [WorkerSolutionRef] on solutions) versus generation
 * phase (parent ref present) and dispatches accordingly. Per-generation work bundles
 * rebalancing imports, mutation tasks, and discards into a single [NodeBatch] per
 * worker via [MutationEvaluator.executeNodeBatches]. Coroutine calls are bridged with
 * [runBlocking] because MOEA Framework invokes callbacks synchronously.
 *
 * @param evaluator The mutation evaluator that performs the actual work (local or federated).
 */
class EvaluationCoordinator(
    private val evaluator: MutationEvaluator
) {
    private val logger = LoggerFactory.getLogger(EvaluationCoordinator::class.java)

    private val allKnownRefs = mutableSetOf<WorkerSolutionRef>()
    private var pendingDiscards = mutableListOf<WorkerSolutionRef>()
    private var pendingRebalancePlan: List<RebalanceTransfer> = emptyList()
    /**
     * The population passed to [prepareIteration], cached so that [handleGeneration] can update
     * [WorkerSolutionRef] attributes on surviving parent solutions after rebalancing.
     *
     * Valid because [prepareIteration] is called before `super.iterate()`, and
     * [handleGeneration] runs inside that same `super.iterate()` call before MOEA merges
     * offspring back into the population.
     */
    private var iterationPopulation: Population? = null
    private var lastBatchSize = 0
    private var lastBatchPerNode: Map<String, Int> = emptyMap()
    private var lastRebalancedCount = 0

    /**
     * Identifies solutions removed from the population since the last iteration,
     * queues them for deferred disposal, and pre-computes the rebalancing plan.
     *
     * Call before `super.iterate()` in delegating algorithm subclasses.
     *
     * @param currentPopulation The live population (or archive) to compare against known refs.
     */
    fun prepareIteration(currentPopulation: Population) {
        iterationPopulation = currentPopulation

        val liveRefs = currentPopulation.mapNotNull { it.getWorkerRef() }.toSet()
        val discarded = allKnownRefs - liveRefs
        if (discarded.isNotEmpty()) {
            logger.debug("Queuing {} solutions for deferred discard", discarded.size)
            pendingDiscards.addAll(discarded)
            allKnownRefs.removeAll(discarded)
        }

        pendingRebalancePlan = PopulationRebalancer.computeTransferPlan(
            allKnownRefs = allKnownRefs,
            nodeIds = evaluator.getNodeIds().toList(),
            lastBatchPerNode = lastBatchPerNode
        )
        if (pendingRebalancePlan.isNotEmpty()) {
            logger.info(
                "Rebalancing plan: {} transfers, {} solutions total",
                pendingRebalancePlan.size,
                pendingRebalancePlan.sumOf { it.solutionIds.size }
            )
        }
    }

    /**
     * Evaluates a batch of solutions by dispatching unified per-node work batches
     * to the underlying [MutationEvaluator].
     *
     * Detects initialization phase (no [WorkerSolutionRef]) versus generation phase
     * (parent ref present) and delegates to [handleInitialization] or [handleGeneration].
     *
     * @param solutions The solutions to evaluate.
     * @return The number of solutions processed.
     */
    fun batchEvaluateAndUpdate(solutions: Iterable<Solution>): Int {
        val solutionList = solutions.toList()
        if (solutionList.isEmpty()) {
            return 0
        }

        val first = solutionList[0]
        val isGeneration = first.getWorkerRef() != null
            || first.getAttribute(PassThroughVariation.PARENT_REF_KEY) != null
        if (isGeneration) {
            handleGeneration(solutionList)
        } else {
            handleInitialization(solutionList)
        }
        return solutionList.size
    }

    /**
     * Evaluates a single solution. Delegates to [batchEvaluateAndUpdate] with a singleton list.
     *
     * @param solution The solution to evaluate.
     */
    fun singleEvaluateAndUpdate(solution: Solution) {
        batchEvaluateAndUpdate(listOf(solution))
    }

    /**
     * Returns a snapshot of all [WorkerSolutionRef]s currently tracked as live by this coordinator.
     *
     * @return An immutable copy of the live reference set.
     */
    fun getAllKnownRefs(): Set<WorkerSolutionRef> = allKnownRefs.toSet()

    /**
     * Returns the total number of live solutions and a per-node breakdown.
     *
     * @return Pair of (total count, per-nodeId count map).
     */
    fun getMetricsSnapshot(): Pair<Int, Map<String, Int>> {
        val perNode = allKnownRefs.groupBy { it.nodeId }.mapValues { it.value.size }
        return Pair(allKnownRefs.size, perNode)
    }

    /**
     * Returns the size and per-node breakdown of the last batch dispatched to workers.
     *
     * @return Pair of (total batch size, per-nodeId task count map).
     */
    fun getLastBatchInfo(): Pair<Int, Map<String, Int>> = Pair(lastBatchSize, lastBatchPerNode)

    /**
     * Returns the number of solutions moved between nodes during the last generation's rebalancing.
     *
     * @return Count of solutions transferred; zero if no rebalancing occurred.
     */
    fun getLastRebalancedCount(): Int = lastRebalancedCount

    /**
     * Handles the initialization phase: flushes any pending discards, calls
     * [MutationEvaluator.initialize], and maps results onto the provided MOEA solutions.
     *
     * @param solutions Empty MOEA solutions awaiting initial worker refs and fitness values.
     */
    private fun handleInitialization(solutions: List<Solution>) {
        if (pendingDiscards.isNotEmpty()) {
            val discards = pendingDiscards.toList()
            pendingDiscards.clear()
            runBlocking {
                val emptyBatches = evaluator.getNodeIds().map { nodeId ->
                    NodeBatch(nodeId, emptyList(), emptyList(), discards.filter { it.nodeId == nodeId }.map { it.solutionId })
                }
                evaluator.executeNodeBatches(emptyBatches)
            }
        }

        val results = runBlocking { evaluator.initialize(solutions.size) }

        for ((index, solution) in solutions.withIndex()) {
            if (index >= results.size) break
            val result = results[index]
            val ref = WorkerSolutionRef(nodeId = result.workerNodeId, solutionId = result.solutionId)
            applyFitness(solution, result.objectives, result.constraints)
            solution.setAttribute(WorkerSolutionRef.ATTRIBUTE_KEY, ref)
            allKnownRefs.add(ref)
        }

        lastBatchSize = results.size
        lastBatchPerNode = results.groupBy { it.workerNodeId }.mapValues { it.value.size }
    }

    /**
     * Dispatches a generation evaluation: builds mutation tasks, fetches rebalance data,
     * assembles per-node batches, executes them, and applies results back to the solutions.
     *
     * @param solutions Offspring solutions carrying parent [WorkerSolutionRef] attributes.
     */
    private fun handleGeneration(solutions: List<Solution>) {
        val mutationTasks = buildMutationTasks(solutions)

        val rebalancePlan = pendingRebalancePlan
        pendingRebalancePlan = emptyList()
        val (importsByDestNode, rebalanceDiscardsByNode) = fetchRebalanceData(rebalancePlan)

        val discards = pendingDiscards.toList()
        pendingDiscards.clear()

        val batches = buildNodeBatches(mutationTasks, importsByDestNode, rebalanceDiscardsByNode, discards)
        val results = runBlocking { evaluator.executeNodeBatches(batches) }

        val populationBySolutionId = buildPopulationLookup()
        applyRebalanceUpdates(rebalancePlan, populationBySolutionId)
        applyMutationResults(solutions, results)

        lastBatchSize = results.size
        lastBatchPerNode = results.filter { it.succeeded }.groupBy { it.workerNodeId }.mapValues { it.value.size }
        lastRebalancedCount = rebalancePlan.sumOf { it.solutionIds.size }
    }

    /**
     * Extracts [MutationTask]s from the offspring solutions, using their stored parent refs.
     * Solutions missing a parent ref are skipped with a warning.
     *
     * @param solutions Offspring solutions produced by the variation operator.
     * @return One [MutationTask] per solution that has a valid parent ref.
     */
    private fun buildMutationTasks(solutions: List<Solution>): List<MutationTask> {
        return solutions.mapNotNull { solution ->
            val parentRef = solution.getAttribute(PassThroughVariation.PARENT_REF_KEY) as? WorkerSolutionRef
            if (parentRef != null) {
                MutationTask(solutionId = parentRef.solutionId, workerNodeId = parentRef.nodeId)
            } else {
                logger.warn("Solution missing parent reference during generation phase")
                null
            }
        }
    }

    /**
     * Pre-fetches serialized models for all solutions in the rebalance plan and organises the
     * results into per-destination import lists and per-source discard lists.
     *
     * Solutions are fetched using [MutationEvaluator.getSolutionDataBatch], which groups them
     * by source node and issues one batched request per worker — reducing the number of
     * round trips compared to individual fetches.
     *
     * @param rebalancePlan The transfers planned by [PopulationRebalancer].
     * @return Pair of (importsByDestNode, rebalanceDiscardsByNode).
     */
    private fun fetchRebalanceData(
        rebalancePlan: List<RebalanceTransfer>
    ): Pair<Map<String, List<SolutionImportData>>, Map<String, List<String>>> {
        if (rebalancePlan.isEmpty()) return Pair(emptyMap(), emptyMap())

        val importsByDestNode = mutableMapOf<String, MutableList<SolutionImportData>>()
        val rebalanceDiscardsByNode = mutableMapOf<String, MutableList<String>>()

        val allRefs = rebalancePlan.flatMap { transfer ->
            transfer.solutionIds.map { solutionId ->
                WorkerSolutionRef(transfer.sourceNodeId, solutionId)
            }
        }
        val fetchedModels = runBlocking { evaluator.getSolutionDataBatch(allRefs) }

        for (transfer in rebalancePlan) {
            for (solutionId in transfer.solutionIds) {
                val serializedModel = fetchedModels[solutionId]
                    ?: error("Missing fetched model for solution $solutionId")
                importsByDestNode.getOrPut(transfer.destinationNodeId) { mutableListOf() }
                    .add(SolutionImportData(solutionId, serializedModel))
            }
            rebalanceDiscardsByNode.getOrPut(transfer.sourceNodeId) { mutableListOf() }
                .addAll(transfer.solutionIds)
        }
        return Pair(importsByDestNode, rebalanceDiscardsByNode)
    }

    /**
     * Assembles one [NodeBatch] per worker node, combining mutation tasks, rebalance imports,
     * and all pending discards (regular + rebalance-source).
     *
     * @param mutationTasks Tasks to dispatch this generation.
     * @param importsByDestNode Inline model data keyed by destination node.
     * @param rebalanceDiscardsByNode Solution IDs to drop from source nodes after transfer.
     * @param discards Regular discards (solutions removed from the population).
     * @return One [NodeBatch] per node in [MutationEvaluator.getNodeIds].
     */
    private fun buildNodeBatches(
        mutationTasks: List<MutationTask>,
        importsByDestNode: Map<String, List<SolutionImportData>>,
        rebalanceDiscardsByNode: Map<String, List<String>>,
        discards: List<WorkerSolutionRef>
    ): List<NodeBatch> {
        val discardsByNode = discards.groupBy({ it.nodeId }, { it.solutionId })
        return evaluator.getNodeIds().map { nodeId ->
            NodeBatch(
                nodeId = nodeId,
                imports = importsByDestNode[nodeId] ?: emptyList(),
                tasks = mutationTasks.filter { it.workerNodeId == nodeId },
                discards = (discardsByNode[nodeId] ?: emptyList()) +
                    (rebalanceDiscardsByNode[nodeId] ?: emptyList())
            )
        }
    }

    /**
     * Builds a lookup map from solution ID to MOEA [Solution] for the current population.
     *
     * @return Map of solutionId → MOEA solution for all solutions with a [WorkerSolutionRef].
     */
    private fun buildPopulationLookup(): Map<String, Solution> {
        return iterationPopulation
            ?.associateBy { sol -> sol.getWorkerRef()?.solutionId ?: "" }
            ?.filterKeys { it.isNotEmpty() }
            ?: emptyMap()
    }

    /**
     * Updates [WorkerSolutionRef] attributes on population solutions and [allKnownRefs]
     * to reflect the new node assignments after rebalancing.
     *
     * @param rebalancePlan The executed rebalance transfers.
     * @param populationBySolutionId Current population keyed by solution ID.
     */
    private fun applyRebalanceUpdates(
        rebalancePlan: List<RebalanceTransfer>,
        populationBySolutionId: Map<String, Solution>
    ) {
        for (transfer in rebalancePlan) {
            for (solutionId in transfer.solutionIds) {
                val oldRef = WorkerSolutionRef(transfer.sourceNodeId, solutionId)
                val newRef = WorkerSolutionRef(transfer.destinationNodeId, solutionId)
                allKnownRefs.remove(oldRef)
                allKnownRefs.add(newRef)
                populationBySolutionId[solutionId]?.setAttribute(WorkerSolutionRef.ATTRIBUTE_KEY, newRef)
            }
            logger.debug(
                "Rebalanced {} solutions: {} → {}",
                transfer.solutionIds.size, transfer.sourceNodeId, transfer.destinationNodeId
            )
        }
    }

    /**
     * Applies fitness values and new [WorkerSolutionRef]s from worker results back onto
     * the MOEA offspring solutions. Penalty fitness is applied for failed evaluations.
     *
     * @param solutions The offspring solutions in the same order as the original task list.
     * @param results Evaluation results returned by the workers.
     */
    private fun applyMutationResults(solutions: List<Solution>, results: List<EvaluationResult>) {
        for ((index, solution) in solutions.withIndex()) {
            val result = results.getOrNull(index)
            if (result != null && result.succeeded) {
                val newRef = WorkerSolutionRef(nodeId = result.workerNodeId, solutionId = result.newSolutionId)
                applyFitness(solution, result.objectives, result.constraints)
                solution.setAttribute(WorkerSolutionRef.ATTRIBUTE_KEY, newRef)
                allKnownRefs.add(newRef)
            } else {
                applyPenaltyFitness(solution)
            }
        }
    }

    /**
     * Writes objective and constraint values from the worker result onto the MOEA solution.
     *
     * @param solution The MOEA solution to update.
     * @param objectives Objective values, applied up to [Solution.numberOfObjectives].
     * @param constraints Constraint values, applied up to [Solution.numberOfConstraints].
     */
    private fun applyFitness(solution: Solution, objectives: List<Double>, constraints: List<Double>) {
        for (i in objectives.indices) {
            if (i < solution.numberOfObjectives) solution.setObjectiveValue(i, objectives[i])
        }
        for (i in constraints.indices) {
            if (i < solution.numberOfConstraints) solution.setConstraintValue(i, constraints[i])
        }
    }

    /**
     * Applies [Double.MAX_VALUE] to all objectives and constraints on [solution],
     * effectively making it dominated by any valid solution.
     *
     * @param solution The MOEA solution to penalise.
     */
    private fun applyPenaltyFitness(solution: Solution) {
        for (i in 0 until solution.numberOfObjectives) solution.setObjectiveValue(i, Double.MAX_VALUE)
        for (i in 0 until solution.numberOfConstraints) solution.setConstraintValue(i, Double.MAX_VALUE)
    }
}

/**
 * Returns the [WorkerSolutionRef] stored as an attribute on this MOEA solution, or `null` if absent.
 */
fun Solution.getWorkerRef(): WorkerSolutionRef? =
    getAttribute(WorkerSolutionRef.ATTRIBUTE_KEY) as? WorkerSolutionRef

