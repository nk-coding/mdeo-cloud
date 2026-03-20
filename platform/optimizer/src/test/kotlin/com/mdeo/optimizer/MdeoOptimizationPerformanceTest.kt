package com.mdeo.optimizer

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.graph.MdeoModelGraph
import com.mdeo.modeltransformation.graph.ModelGraph

class MdeoOptimizationPerformanceTest : OptimizationPerformanceTestBase() {
    override val backendName: String = "MdeoModelGraph"

    override fun createModelGraph(modelData: ModelData, metamodel: Metamodel): ModelGraph =
        MdeoModelGraph.create(modelData, metamodel)
}
