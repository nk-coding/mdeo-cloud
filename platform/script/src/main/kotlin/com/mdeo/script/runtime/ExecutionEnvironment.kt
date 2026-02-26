package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompiledProgram
import java.lang.reflect.Method
import java.lang.reflect.InvocationTargetException

/**
 * Environment for executing compiled script programs.
 *
 * Provides methods to invoke functions from compiled scripts.
 *
 * @param program The compiled program to execute.
 */
class ExecutionEnvironment(
    val program: CompiledProgram
) {
    /**
     * The class loader for loading compiled classes.
     * Exposed so callers can reuse the same loader (e.g. for creating model instances
     * that will be cast to generated types inside the executing script).
     */
    val classLoader = ScriptClassLoader(program, ExecutionEnvironment::class.java.classLoader)
    
    /**
     * Invokes a function from a specific file.
     *
     * @param filePath The file path containing the function.
     * @param functionName The name of the function to invoke.
     * @param args The arguments to pass to the function.
     * @return The result of the function invocation, or null if the function returns void.
     * @throws IllegalArgumentException if the file or function is not found.
     */
    fun invoke(filePath: String, functionName: String, vararg args: Any?): Any? {
        val clazz = classLoader.loadClassForFile(filePath)

        val method = findMethod(clazz, functionName, args)
            ?: throw IllegalArgumentException("Function not found: $functionName in $filePath")

        try {
            return method.invoke(null, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }
    
    /**
     * Finds a method by name and matching parameter count.
     * 
     * @param clazz The class to search in.
     * @param methodName The method name.
     * @param args The arguments (used to determine parameter count).
     * @return The matching Method, or null if not found.
     */
    private fun findMethod(clazz: Class<*>, methodName: String, args: Array<out Any?>): Method? {
        return clazz.methods.find { method ->
            method.name == methodName && method.parameterCount == args.size
        }
    }
    
    /**
     * Gets a loaded class by file path.
     * 
     * @param filePath The file path.
     * @return The loaded Class.
     */
    fun getClass(filePath: String): Class<*> {
        return classLoader.loadClassForFile(filePath)
    }
}
