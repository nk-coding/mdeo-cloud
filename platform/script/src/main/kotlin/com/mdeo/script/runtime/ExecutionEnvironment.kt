package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompiledProgram
import java.lang.reflect.InvocationTargetException

/**
 * A handle to a single instance of the compiled [ScriptProgram] class.
 *
 * All script functions from all files are methods on this single instance.
 * Use [invoke] to call any function by its original (filePath, functionName) identity.
 *
 * @param instance       The underlying Java object (an instance of the ScriptProgram class).
 * @param clazz          The loaded ScriptProgram class.
 * @param functionLookup Maps (filePath → functionName → jvmMethodName).
 */
class ScriptProgramInstance(
    private val instance: Any,
    private val clazz: Class<*>,
    private val functionLookup: Map<String, Map<String, String>>
) {
    /**
     * Invokes a script function identified by its original file path and function name.
     *
     * @param filePath     The script file path containing the function.
     * @param functionName The original function name as written in the script.
     * @param args         Arguments to pass to the function.
     * @return The function's return value, or null for void functions.
     * @throws IllegalArgumentException if the function is not found.
     */
    fun invoke(filePath: String, functionName: String, vararg args: Any?): Any? {
        val jvmName = functionLookup[filePath]?.get(functionName)
            ?: throw IllegalArgumentException("Function '$functionName' not found in file '$filePath'")
        val method = clazz.methods.find { it.name == jvmName && it.parameterCount == args.size }
            ?: throw IllegalArgumentException("JVM method '$jvmName' not found with ${args.size} params")
        try {
            return method.invoke(instance, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }
}

/**
 * Environment for executing compiled script programs.
 *
 * Provides methods to invoke functions from compiled scripts.
 * Use [createInstance] to create a [ScriptProgramInstance] that can evaluate any
 * function from any file in the compiled program with a shared [ScriptContext].
 * Use [invoke] for simple single-function invocations.
 *
 * @param program The compiled program to execute.
 */
class ExecutionEnvironment(
    val program: CompiledProgram
) {
    /**
     * The class loader for loading compiled classes.
     *
     * When a metamodel is present, the metamodel's class loader is used as the parent
     * so that generated metamodel classes (instance classes, enum classes, etc.) are
     * found via normal parent delegation.
     */
    val classLoader = ScriptClassLoader(
        program,
        program.metamodel?.classLoader ?: ExecutionEnvironment::class.java.classLoader
    )

    /**
     * The single compiled [ScriptProgram] class containing all script functions.
     */
    val scriptProgramClass: Class<*> get() = classLoader.loadScriptProgramClass()

    /**
     * Creates a [ScriptProgramInstance] bound to the given [context].
     *
     * The returned instance can evaluate any function from any compiled file.
     * Reuse the same instance across multiple function calls within one evaluation
     * to share the [ScriptContext] (e.g. when evaluating all objectives and constraints
     * for a single optimizer candidate).
     *
     * @param context The script execution context providing printStream and model.
     * @return A [ScriptProgramInstance] ready to invoke functions.
     */
    fun createInstance(context: ScriptContext): ScriptProgramInstance {
        val clazz = classLoader.loadScriptProgramClass()
        val constructor = clazz.getDeclaredConstructor(ScriptContext::class.java)
        val instance = constructor.newInstance(context)
        return ScriptProgramInstance(instance, clazz, program.functionLookup)
    }

    /**
     * Invokes a function from a specific file with the given [ScriptContext].
     *
     * Creates a new [ScriptProgramInstance] for the invocation.
     *
     * @param filePath The file path containing the function.
     * @param functionName The name of the function to invoke.
     * @param context The script execution context providing printStream and model.
     * @param args The arguments to pass to the function.
     * @return The result of the function invocation, or null if the function returns void.
     * @throws IllegalArgumentException if the file or function is not found.
     */
    fun invoke(filePath: String, functionName: String, context: ScriptContext, vararg args: Any?): Any? {
        return createInstance(context).invoke(filePath, functionName, *args)
    }

}
