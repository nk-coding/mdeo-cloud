package com.mdeo.optimizer.moea

import com.mdeo.optimizer.evaluation.WorkerSolutionRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PopulationRebalancerTest {

    @Test
    fun `no transfers when single node`() {
        val refs = (1..10).map { WorkerSolutionRef("node-0", "sol-$it") }.toSet()
        val plan = PopulationRebalancer.computeTransferPlan(refs, listOf("node-0"), emptyMap())
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `no transfers when balanced`() {
        val refs = setOf(
            WorkerSolutionRef("a", "s1"),
            WorkerSolutionRef("a", "s2"),
            WorkerSolutionRef("b", "s3"),
            WorkerSolutionRef("b", "s4")
        )
        val plan = PopulationRebalancer.computeTransferPlan(refs, listOf("a", "b"), emptyMap())
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `no transfers when imbalance below threshold`() {
        // 11 vs 9 out of 20 → diff = 2, ideal = 10, threshold = max(3, 10*0.25=2) = 3
        val refs = (1..11).map { WorkerSolutionRef("a", "a-$it") }.toSet() +
            (1..9).map { WorkerSolutionRef("b", "b-$it") }.toSet()
        val plan = PopulationRebalancer.computeTransferPlan(refs, listOf("a", "b"), emptyMap())
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `transfers when significant imbalance`() {
        // 30 on node a, 10 on node b → clearly imbalanced
        val refs = (1..30).map { WorkerSolutionRef("a", "a-$it") }.toSet() +
            (1..10).map { WorkerSolutionRef("b", "b-$it") }.toSet()
        val plan = PopulationRebalancer.computeTransferPlan(refs, listOf("a", "b"), emptyMap())
        assertFalse(plan.isEmpty(), "Should trigger rebalancing when 30 vs 10")

        val totalMoved = plan.sumOf { it.solutionIds.size }
        // ideal = 20 each, so should move 10 from a to b
        assertEquals(10, totalMoved)
        assertTrue(plan.all { it.sourceNodeId == "a" && it.destinationNodeId == "b" })
    }

    @Test
    fun `transfers with three nodes`() {
        // 40 on a, 5 on b, 5 on c → ideal = 16/17 each
        val refs = (1..40).map { WorkerSolutionRef("a", "a-$it") }.toSet() +
            (1..5).map { WorkerSolutionRef("b", "b-$it") }.toSet() +
            (1..5).map { WorkerSolutionRef("c", "c-$it") }.toSet()
        val plan = PopulationRebalancer.computeTransferPlan(refs, listOf("a", "b", "c"), emptyMap())
        assertFalse(plan.isEmpty())

        // After rebalancing, each node should end up close to 50/3 ≈ 16-17
        val nodeCountsAfter = mutableMapOf("a" to 40, "b" to 5, "c" to 5)
        for (transfer in plan) {
            nodeCountsAfter[transfer.sourceNodeId] = nodeCountsAfter[transfer.sourceNodeId]!! - transfer.solutionIds.size
            nodeCountsAfter[transfer.destinationNodeId] = nodeCountsAfter[transfer.destinationNodeId]!! + transfer.solutionIds.size
        }

        val maxAfter = nodeCountsAfter.values.max()
        val minAfter = nodeCountsAfter.values.min()
        assertTrue(maxAfter - minAfter <= 2, "Nodes should be within 2 of each other after rebalancing, got $nodeCountsAfter")
    }

    @Test
    fun `handles empty refs`() {
        val plan = PopulationRebalancer.computeTransferPlan(emptySet(), listOf("a", "b"), emptyMap())
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `handles node with zero solutions`() {
        // All on a, none on b → should rebalance
        val refs = (1..20).map { WorkerSolutionRef("a", "a-$it") }.toSet()
        val plan = PopulationRebalancer.computeTransferPlan(refs, listOf("a", "b"), emptyMap())
        assertFalse(plan.isEmpty())

        val movedToB = plan.filter { it.destinationNodeId == "b" }.sumOf { it.solutionIds.size }
        assertEquals(10, movedToB, "Should move 10 solutions to the empty node")
    }

    @Test
    fun `transferred solution ids are from the donor node`() {
        val refs = (1..30).map { WorkerSolutionRef("a", "a-$it") }.toSet() +
            (1..10).map { WorkerSolutionRef("b", "b-$it") }.toSet()
        val plan = PopulationRebalancer.computeTransferPlan(refs, listOf("a", "b"), emptyMap())

        val allDonorSolutionIds = refs.filter { it.nodeId == "a" }.map { it.solutionId }.toSet()
        for (transfer in plan) {
            for (id in transfer.solutionIds) {
                assertTrue(id in allDonorSolutionIds, "Transferred solution '$id' should be from donor node")
            }
        }
    }
}
