package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompiledProgram
import java.io.PrintStream
import java.lang.reflect.Method

/**
 * Environment for executing compiled script programs.
 *
 * Provides methods to invoke functions from compiled scripts
 * and manages the console output stream.
 *
 * @param program The compiled program to execute.
 * @param console The PrintStream for console output.
 */
class ExecutionEnvironment(
    private val program: CompiledProgram,
    val console: PrintStream = System.out
) {
    /**
     * The class loader for loading compiled classes.
     */
    private val classLoader = ScriptClassLoader(program)
    
    /**
     * Invokes a function from a specific file.
     * 
     * Sets up the execution context with the console stream before invoking,
     * so that println and other output functions write to the correct stream.
     * 
     * @param fileUri The file URI containing the function.
     * @param functionName The name of the function to invoke.
     * @param args The arguments to pass to the function.
     * @return The result of the function invocation, or null if the function returns void.
     * @throws IllegalArgumentException if the file or function is not found.
     */
    fun invoke(fileUri: String, functionName: String, vararg args: Any?): Any? {
        return ExecutionContext.withConsole(console) {
            val clazz = classLoader.loadClassForFile(fileUri)
            
            val method = findMethod(clazz, functionName, args)
                ?: throw IllegalArgumentException("Function not found: $functionName in $fileUri")
            
            method.invoke(null, *args)
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
     * Gets a loaded class by file URI.
     * 
     * @param fileUri The file URI.
     * @return The loaded Class.
     */
    fun getClass(fileUri: String): Class<*> {
        return classLoader.loadClassForFile(fileUri)
    }
}
