package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.compiler.CompilationResult
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Holds compiled variable expressions to avoid recompilation.
 *
 * Variables in patterns are compiled once during match clause building and may be
 * referenced later during property constraint evaluation. This data structure caches
 * the compilation results to avoid compiling the same expression multiple times.
 *
 * Each variable is identified by its label (as computed by VariableBinding.variableLabel),
 * and we store both the compilation result and the compiled traversal for convenience.
 *
 * ## Usage Pattern
 *
 * 1. During match clause building, compile all variables and store in this structure
 * 2. When evaluating property == expressions, check if the expression is a variable reference
 * 3. If yes, retrieve the pre-compiled result instead of recompiling
 *
 * @property results Map from variable label to its compilation result
 */
internal data class CompiledVariables(
    val results: Map<String, CompilationResult>
) {
    /**
     * Gets the compilation result for a variable by its label.
     *
     * @param variableLabel The label of the variable (from VariableBinding.variableLabel)
     * @return The compilation result, or null if the variable was not compiled
     */
    fun get(variableLabel: String): CompilationResult? {
        return results[variableLabel]
    }
    
    /**
     * Gets the traversal for a variable by its label.
     *
     * @param variableLabel The label of the variable (from VariableBinding.variableLabel)
     * @return The compiled traversal, or null if the variable was not compiled
     */
    fun getTraversal(variableLabel: String): GraphTraversal<*, *>? {
        return results[variableLabel]?.traversal
    }
    
    /**
     * Checks if a variable label has been compiled.
     *
     * @param variableLabel The label of the variable to check
     * @return true if the variable has been compiled
     */
    fun contains(variableLabel: String): Boolean {
        return variableLabel in results
    }
    
    companion object {
        /**
         * Creates an empty CompiledVariables instance.
         *
         * @return An empty CompiledVariables with no compiled results
         */
        fun empty(): CompiledVariables {
            return CompiledVariables(emptyMap())
        }
    }
}
