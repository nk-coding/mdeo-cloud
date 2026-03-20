package com.mdeo.optimizer.guidance

import com.mdeo.optimizer.solution.Solution
import com.mdeo.script.runtime.ScriptContext
import com.mdeo.script.runtime.SimpleScriptContext
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException

class ScriptGuidanceFunction(
    private val clazz: Class<*>,
    private val jvmMethodName: String,
    private val printStream: PrintStream,
    override val name: String
) : GuidanceFunction {

    private val logger = LoggerFactory.getLogger(ScriptGuidanceFunction::class.java)

    override fun computeFitness(solution: Solution): Double {
        val model = solution.modelGraph.toModel()
        val context = SimpleScriptContext(printStream, model)
        val instance = clazz.getDeclaredConstructor(ScriptContext::class.java).newInstance(context)
        val method = clazz.methods.find { it.name == jvmMethodName && it.parameterCount == 0 }
            ?: error("JVM method '$jvmMethodName' not found in class '${clazz.name}'")
        val result = try {
            method.invoke(instance)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
        return toDouble(result)
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
