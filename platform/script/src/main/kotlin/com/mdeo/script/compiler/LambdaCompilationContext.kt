package com.mdeo.script.compiler

/**
 * Context for lambda compilation that tracks synthetic method names.
 * 
 * This class maintains a counter for unique synthetic method naming
 * following Java's lambda naming convention (lambda$context$N).
 * 
 * It also maintains a stack of method names to support nested lambdas,
 * where each nested lambda uses the parent's method name as context.
 */
class LambdaCompilationContext(
    /**
     * The base class name for the current file being compiled.
     */
    private val baseClassName: String
) {
    /**
     * Counter for generating unique synthetic method names.
     */
    private var lambdaCounter: Int = 0
    
    /**
     * Stack of synthetic method names being compiled.
     * Used to track the current lambda when nested lambdas are involved.
     * When a lambda starts compilation, its method name is pushed.
     * When compilation finishes, it's popped.
     */
    private val lambdaMethodNameStack: MutableList<String> = mutableListOf()
    
    /**
     * Generates a unique synthetic method name for a lambda.
     * Uses Java-style naming: lambda$script$N for top-level or lambda$functionName$N.
     * 
     * @param context The context name (function name or "script" for top-level).
     * @return A unique method name like "lambda$script$0".
     */
    fun generateLambdaMethodName(context: String = "script"): String {
        val name = "lambda\$${context}\$${lambdaCounter++}"
        lambdaMethodNameStack.add(name)
        return name
    }
    
    /**
     * Signals that lambda compilation is complete and pops the method name from the stack.
     * 
     * @return The method name of the lambda that just finished compilation.
     */
    fun finishLambdaCompilation(): String {
        return lambdaMethodNameStack.removeAt(lambdaMethodNameStack.lastIndex)
    }
    
    /**
     * Gets the method name of the currently compiling lambda (top of stack).
     * 
     * @return The current lambda method name, or null if not in a lambda.
     */
    fun getCurrentLambdaMethodName(): String? {
        return lambdaMethodNameStack.lastOrNull()
    }
}
