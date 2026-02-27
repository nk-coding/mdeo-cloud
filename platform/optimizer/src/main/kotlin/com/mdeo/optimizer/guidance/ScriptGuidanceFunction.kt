package com.mdeo.optimizer.guidance

import com.mdeo.optimizer.graph.GraphBackend
import com.mdeo.optimizer.solution.Solution
import com.mdeo.script.runtime.ExecutionContext
import com.mdeo.script.runtime.ExecutionEnvironment
import com.mdeo.script.runtime.model.ScriptModel
import org.slf4j.LoggerFactory

/**
 * A [GuidanceFunction] backed by a compiled script function.
 *
 * The script function is a no-arg function that accesses the model through
 * [ExecutionContext.requireModel()]. Before invocation, this adapter sets
 * up the context with a [ScriptModel] backed by the candidate's graph.
 *
 * @param environment The compiled script execution environment.
 * @param filePath Path of the script file containing the function.
 * @param functionName Name of the no-arg function to invoke.
 * @param metamodelData Metamodel used to create the script model.
 * @param graphToModelData Converter from graph to ModelData (for script model creation).
 * @param conversionStrategy Whether to eagerly or lazily convert model data.
 * @param scriptModelFactory Factory for creating ScriptModel instances from the graph.
 */
class ScriptGuidanceFunction(
    private val environment: ExecutionEnvironment,
    private val filePath: String,
    private val functionName: String,
    private val scriptModelFactory: ScriptModelFactory
) : GuidanceFunction {

    private val logger = LoggerFactory.getLogger(ScriptGuidanceFunction::class.java)

    override val name: String get() = "$filePath::$functionName"

    override fun computeFitness(solution: Solution): Double {
        val model = scriptModelFactory.create(solution.graphBackend)

        return ExecutionContext.withContext(System.out, model) {
            val result = environment.invoke(filePath, functionName)
            toDouble(result)
        }
    }

    private fun toDouble(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Number -> value.toDouble()
            is Boolean -> if (value) 1.0 else 0.0
            null -> 0.0
            else -> {
                logger.warn("Guidance function $name returned non-numeric value: $value")
                0.0
            }
        }
    }
}

/**
 * Factory for creating [ScriptModel] instances from a [GraphBackend].
 *
 * This indirection allows switching between eager and lazy conversion strategies,
 * and also supports future Gremlin-native ScriptModel implementations.
 */
interface ScriptModelFactory {
    fun create(graphBackend: GraphBackend): ScriptModel
}
