package com.mdeo.optimizer

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.graph.ModelGraph
import com.mdeo.modeltransformation.graph.tinker.TinkerModelGraph

class TinkerScrumOptimizationPerformanceTest : ScrumOptimizationPerformanceTestBase() {
    override val backendName: String = "TinkerModelGraph"

    override fun createModelGraph(modelData: ModelData, metamodel: Metamodel): ModelGraph =
        TinkerModelGraph.create(modelData, metamodel)
}
